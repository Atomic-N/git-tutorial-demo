<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="page-title-row">
    <div>
        <h1>标签</h1>
        <p class="muted">用标签聚合模型、工具、实践和资源。</p>
    </div>
</section>

<section class="tag-groups">
    <c:forEach var="entry" items="${tagGroups}">
        <div class="tag-group">
            <h2><c:out value="${entry.key}"/></h2>
            <c:if test="${not empty tagGroupCatalog[entry.key] && not empty tagGroupCatalog[entry.key].description}">
                <p class="muted"><c:out value="${tagGroupCatalog[entry.key].description}"/></p>
            </c:if>
            <div class="tag-cloud">
                <c:forEach var="tagItem" items="${entry.value}">
                    <a class="tag-chip" href="${pageContext.request.contextPath}/tag/${tagItem.slug}">
                        <i data-lucide="${tagItem.icon}" style="color:${tagItem.color}"></i>
                        <span><c:out value="${tagItem.name}"/></span>
                        <b>x ${tagItem.topicCount}</b>
                    </a>
                </c:forEach>
            </div>
        </div>
    </c:forEach>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
