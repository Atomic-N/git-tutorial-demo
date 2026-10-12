package com.aitef.forum.controller.search;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.dto.SearchCriteria;
import com.aitef.forum.model.service.CatalogService;
import com.aitef.forum.model.service.TopicService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/search")
public class SearchServlet extends BaseServlet {
    private final TopicService topicService = new TopicService();
    private final CatalogService catalogService = new CatalogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setKeyword(stringParam(request, "q"));
        criteria.setAuthor(stringParam(request, "author"));
        criteria.setCategoryId(longParam(request, "categoryId"));
        criteria.setTagId(longParam(request, "tagId"));
        criteria.setSolvedOnly(request.getParameter("solvedOnly") != null);
        criteria.setCreatedWithinDays(nullableInt(request, "days"));
        criteria.setMinReplies(nullableInt(request, "minReplies"));
        criteria.setMaxReplies(nullableInt(request, "maxReplies"));
        criteria.setMinViews(nullableInt(request, "minViews"));
        criteria.setMaxViews(nullableInt(request, "maxViews"));
        criteria.setOrderBy(stringParam(request, "orderBy").isBlank() ? "LATEST" : stringParam(request, "orderBy"));
        criteria.setPage(intParam(request, "page", 1));
        criteria.setSize(20);
        request.setAttribute("criteria", criteria);
        request.setAttribute("tags", catalogService.listTags());
        request.setAttribute("topicsPage", topicService.list("latest", criteria, null, currentUserId(request)));
        request.setAttribute("pageTitle", "搜索");
        request.setAttribute("activeNav", "search");
        render(request, response, "search/index.jsp");
    }

    private Integer nullableInt(HttpServletRequest request, String name) {
        try {
            String value = request.getParameter(name);
            return value == null || value.isBlank() ? null : Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
