package ch.timekeeper.service

import ch.timekeeper.domain.Category
import ch.timekeeper.repository.CategoryRepository
import ch.timekeeper.web.NotFoundException
import ch.timekeeper.web.dto.CategoryDto
import ch.timekeeper.web.dto.CategoryRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
) {

    @Transactional(readOnly = true)
    fun findAll(): List<CategoryDto> =
        categoryRepository.findAll().sortedBy { it.name.lowercase() }.map { it.toDto() }

    @Transactional
    fun create(request: CategoryRequest): CategoryDto {
        val name = request.name.trim()
        require(name.isNotEmpty()) { "Category name must not be empty" }
        require(!categoryRepository.existsByNameIgnoreCase(name)) {
            "Category '$name' already exists"
        }
        return categoryRepository.save(Category(name = name, color = request.color)).toDto()
    }

    @Transactional
    fun update(id: Long, request: CategoryRequest): CategoryDto {
        val category = categoryRepository.findById(id)
            .orElseThrow { NotFoundException("Category $id not found") }
        val name = request.name.trim()
        require(name.isNotEmpty()) { "Category name must not be empty" }
        val existing = categoryRepository.findByNameIgnoreCase(name)
        require(existing == null || existing.id == id) { "Category '$name' already exists" }
        category.name = name
        category.color = request.color
        return category.toDto()
    }

    @Transactional
    fun delete(id: Long) {
        val category = categoryRepository.findById(id)
            .orElseThrow { NotFoundException("Category $id not found") }
        categoryRepository.delete(category)
    }

    fun getEntity(id: Long): Category =
        categoryRepository.findById(id)
            .orElseThrow { NotFoundException("Category $id not found") }
}

fun Category.toDto() = CategoryDto(
    id = id!!,
    name = name,
    color = color,
)
