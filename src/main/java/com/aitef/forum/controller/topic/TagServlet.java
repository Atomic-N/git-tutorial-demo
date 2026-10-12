package com.aitef.forum.controller.topic;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.dto.SearchCriteria;
import com.aitef.forum.model.entity.Tag;
import com.aitef.forum.model.entity.TagGroup;
import com.aitef.forum.model.service.CatalogService;
import com.aitef.forum.model.service.TopicService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/tags", "/tag/*"})
public class TagServlet extends BaseServlet {
    private final CatalogService catalogService = new CatalogService();
    private final TopicService topicService = new TopicService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("/tags".equals(request.getServletPath())) {
            request.setAttribute("tagGroups", catalogService.groupTags());
            request.setAttribute("tagGroupCatalog", catalogService.listTagGroups().stream().collect(java.util.stream.Collectors.toMap(TagGroup::getName, item -> item, (a, b) -> a, java.util.LinkedHashMap::new)));
            request.setAttribute("pageTitle", "标签");
            request.setAttribute("activeNav", "tags");
            render(request, response, "tag/list.jsp");
            return;
        }
        String slug = pathAfter(request, "/tag");
        Tag tag = catalogService.findTagBySlug(slug).orElse(null);
        if (tag == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        SearchCriteria criteria = new SearchCriteria();
        criteria.setPage(intParam(request, "page", 1));
        criteria.setSize(20);
        criteria.setTagId(tag.getId());
        request.setAttribute("tag", tag);
        request.setAttribute("topicsPage", topicService.list("latest", criteria, null, currentUserId(request)));
        request.setAttribute("pageTitle", tag.getName());
        request.setAttribute("activeNav", "tags");
        render(request, response, "topic/list.jsp");
    }
}
