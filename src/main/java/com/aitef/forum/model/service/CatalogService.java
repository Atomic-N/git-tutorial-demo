package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.CategoryDao;
import com.aitef.forum.model.dao.TagDao;
import com.aitef.forum.model.dao.TagGroupDao;
import com.aitef.forum.model.entity.Category;
import com.aitef.forum.model.entity.Tag;
import com.aitef.forum.model.entity.TagGroup;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CatalogService {
    private final CategoryDao categoryDao = new CategoryDao();
    private final TagDao tagDao = new TagDao();
    private final TagGroupDao tagGroupDao = new TagGroupDao();

    public List<Category> listCategories() {
        return categoryDao.findAll();
    }

    public List<Category> listVisibleCategories(int trustLevel) {
        return categoryDao.findVisible(trustLevel);
    }

    public Optional<Category> findCategoryBySlug(String slug) {
        return categoryDao.findBySlug(slug);
    }

    public Optional<Category> findCategoryById(Long id) {
        return categoryDao.findById(id);
    }

    public List<Tag> listTags() {
        return tagDao.findAll();
    }

    public Map<String, List<Tag>> groupTags() {
        return tagDao.groupByName();
    }

    public List<TagGroup> listTagGroups() {
        return tagGroupDao.findAll();
    }

    public Optional<Tag> findTagBySlug(String slug) {
        return tagDao.findBySlug(slug);
    }
}
