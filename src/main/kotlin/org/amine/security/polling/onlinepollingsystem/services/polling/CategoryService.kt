package org.amine.security.polling.onlinepollingsystem.services.polling

import org.amine.security.polling.onlinepollingsystem.models.polling.Category
import org.amine.security.polling.onlinepollingsystem.repos.polling.CategoryRepository
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class CategoryService(private val categoryRepository: CategoryRepository) {

    fun saveNewCategory(categoryName: String) {
        val category = Category()
        category.categoryName = categoryName
        categoryRepository.save(category)
    }

    fun getAllCategories(): HashSet<Category> {
        return categoryRepository.findAll().toHashSet()
    }

    @Bean
    fun getAllCategoriesName(): HashSet<String> {
        return getAllCategories().map { it.categoryName }.toHashSet()
    }

    fun findCategory(categoryName: String): Optional<Category> {
        return categoryRepository.findByCategoryName(categoryName)
    }
}