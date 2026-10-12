package com.aitef.forum.controller.topic;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.dto.SearchCriteria;
import com.aitef.forum.model.entity.Category;
import com.aitef.forum.model.service.CatalogService;
import com.aitef.forum.model.service.TopicService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/categories", "/c/*"})
public class CategoryServlet extends BaseServlet {
    private final CatalogService catalogService = new CatalogService();
    private final TopicService topicService = new TopicService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("/categories".equals(request.getServletPath())) {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setPage(1);
            criteria.setSize(8);
            request.setAttribute("latestTopics", topicService.list("latest", criteria, null, currentUserId(request)).getItems());
            request.setAttribute("pageTitle", "类别");
            request.setAttribute("activeNav", "categories");
            render(request, response, "category/list.jsp");
            return;
        }

        String slug = pathAfter(request, "/c");
        Category category = catalogService.findCategoryBySlug(slug).orElse(null);
        if (category == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        int trustLevel = currentUser(request) != null && currentUser(request).isAdmin() ? 99 : (currentUser(request) == null ? 0 : currentUser(request).getTrustLevel());
        if (category.getMinTrustLevel() > trustLevel) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        SearchCriteria criteria = new SearchCriteria();
        criteria.setPage(intParam(request, "page", 1));
        criteria.setSize(20);
        criteria.setCategoryId(category.getId());
        request.setAttribute("category", category);
        request.setAttribute("topicsPage", topicService.list("latest", criteria, null, currentUserId(request)));
        request.setAttribute("pageTitle", category.getName());
        request.setAttribute("activeNav", "categories");
        render(request, response, "topic/list.jsp");
    }
}
