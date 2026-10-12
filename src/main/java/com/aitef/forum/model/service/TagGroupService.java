package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.TagGroupDao;
import com.aitef.forum.model.entity.TagGroup;

import java.util.List;

public class TagGroupService {
    private final TagGroupDao tagGroupDao = new TagGroupDao();

    public List<TagGroup> listAll() {
        return tagGroupDao.findAll();
    }
}
