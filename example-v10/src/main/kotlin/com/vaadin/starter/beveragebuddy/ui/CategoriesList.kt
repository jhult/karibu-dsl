/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.starter.beveragebuddy.ui

import com.vaadin.router.Route
import com.vaadin.router.Title
import com.vaadin.starter.beveragebuddy.backend.Category
import com.vaadin.starter.beveragebuddy.backend.CategoryService
import com.vaadin.starter.beveragebuddy.backend.ReviewService
import com.vaadin.ui.button.Button
import com.vaadin.ui.grid.Grid
import com.vaadin.ui.html.Div
import com.vaadin.ui.icon.Icon
import com.vaadin.ui.icon.VaadinIcons
import com.vaadin.ui.textfield.TextField
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * Displays the list of available categories, with a search filter as well as
 * buttons to add a new category or edit existing ones.
 */
@Route(value = "categories", layout = MainLayout::class)
@Title("Categories List")
class CategoriesList : Div() {

    private val searchField = TextField("", "Search")
    private val grid = Grid<Category>()

    private val form = CategoryEditorDialog(
            BiConsumer<Category, AbstractEditorDialog.Operation> { category, operation -> this.saveCategory(category, operation) }, Consumer<Category> { this.deleteCategory(it) })

    private val notification = PaperToast()

    init {
        initView()

        addSearchBar()
        addGrid()

        updateView()
    }

    private fun initView() {
        addClassName("categories-list")

        notification.addClassName("notification")
        add(notification, form)
    }

    private fun addSearchBar() {
        val viewToolbar = Div()
        viewToolbar.addClassName("view-toolbar")

        searchField.addClassName("view-toolbar__search-field")
        searchField.addValueChangeListener { e -> updateView() }

        val newButton = Button("New category",
                Icon(VaadinIcons.PLUS))
        newButton.element.setAttribute("theme", "primary")
        newButton.addClassName("view-toolbar__button")
        newButton.addClickListener { e ->
            form.open(Category(null, ""),
                    AbstractEditorDialog.Operation.ADD)
        }

        viewToolbar.add(searchField, newButton)
        add(viewToolbar)
    }

    private fun addGrid() {
        grid.addColumn("Category", { it.name })
        grid.addColumn("Beverages", { getReviewCount(it) })
        // Grid does not yet implement HasStyle
        grid.element.classList.add("categories")
        grid.element.setAttribute("theme", "row-dividers")
        grid.asSingleSelect().addValueChangeListener {
            if (it.value != null) {  // deselect fires yet another selection event, this time with null Category.
                selectionChanged(it.value.id!!)
                grid.selectionModel.deselect(it.value)
            }
        }
        add(grid)
    }

    private fun selectionChanged(categoryId: Long) {
        form.open(CategoryService.getById(categoryId), AbstractEditorDialog.Operation.EDIT)
    }

    private fun getReviewCount(category: Category): String {
        val reviewsInCategory = ReviewService.findReviews(category.name)
        val totalCount = reviewsInCategory.sumBy { it.count }
        return totalCount.toString()
    }

    private fun updateView() {
        val categories = CategoryService.findCategories(searchField.value)
        grid.setItems(categories)
    }

    private fun saveCategory(category: Category,
                             operation: AbstractEditorDialog.Operation) {
        CategoryService.saveCategory(category)
        notification.show("Category successfully ${operation.nameInText}ed.")
        updateView()
    }

    private fun deleteCategory(category: Category) {
        val reviewsInCategory = ReviewService.findReviews(category.name)
        reviewsInCategory.forEach { review ->
            review.category = Category.UNDEFINED
            ReviewService.saveReview(review)
        }
        CategoryService.deleteCategory(category)
        notification.show("Category successfully deleted.")
        updateView()
    }
}