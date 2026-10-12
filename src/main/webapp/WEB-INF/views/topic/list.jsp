<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="welcome">
    <c:choose>
        <c:when test="${not empty category}">
            <h1><c:out value="${category.name}"/></h1>
            <p><c:out value="${category.description}"/></p>
        </c:when>
        <c:when test="${not empty tag}">
            <h1>#<c:out value="${tag.name}"/></h1>
            <p>查看带有该标签的 AI 技术讨论。</p>
        </c:when>
        <c:otherwise>
            <h1>Where possible begins</h1>
            <form class="hero-search" action="${pageContext.request.contextPath}/search" method="get">
                <i data-lucide="search"></i>
                <input name="q" placeholder="搜索">
                <button type="submit" title="高级搜索"><i data-lucide="sliders-horizontal"></i></button>
            </form>
        </c:otherwise>
    </c:choose>
</section>

<c:if test="${not empty param.message}">
    <div class="form-success"><c:out value="${param.message}"/></div>
</c:if>

<section class="toolbar-row">
    <div class="filter-buttons">
        <a class="filter-pill" href="${pageContext.request.contextPath}/categories">类别 <i data-lucide="chevron-right"></i></a>
        <a class="filter-pill" href="${pageContext.request.contextPath}/tags">标签 <i data-lucide="chevron-right"></i></a>
    </div>
    <nav class="tabs">
        <a class="${mode == 'latest' || empty mode ? 'active' : ''}" href="${pageContext.request.contextPath}/latest">最新</a>
        <a class="${mode == 'new' ? 'active' : ''}" href="${pageContext.request.contextPath}/new">新</a>
        <a class="${mode == 'unread' ? 'active' : ''}" href="${pageContext.request.contextPath}/unread">未读</a>
        <a class="${mode == 'top' ? 'active' : ''}" href="${pageContext.request.contextPath}/top">排行榜</a>
        <c:if test="${not empty currentUser}">
            <a class="${mode == 'bookmarks' ? 'active' : ''}" href="${pageContext.request.contextPath}/bookmarks">书签</a>
        </c:if>
    </nav>
    <div class="toolbar-actions">
        <c:if test="${not empty currentUser && mode == 'unread'}">
            <form action="${pageContext.request.contextPath}/unread" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <button class="btn btn-ghost" type="submit"><i data-lucide="check-check"></i>全部已读</button>
            </form>
        </c:if>
        <c:if test="${not empty currentUser}">
            <a class="btn btn-muted" href="${pageContext.request.contextPath}/topics/new"><i data-lucide="square-pen"></i>新建话题</a>
        </c:if>
    </div>
</section>

<section class="topic-table">
    <div class="topic-head">
        <span>话题</span>
        <span>回复</span>
        <span>浏览量</span>
        <span>活动</span>
    </div>
    <c:forEach var="topic" items="${topicsPage.items}">
        <article class="topic-row ${topic.unreadByCurrentUser ? 'unread-topic' : ''}">
            <div class="topic-main">
                <div class="topic-title-line">
                    <c:if test="${topic.pinned}"><i class="pin" data-lucide="pin"></i></c:if>
                    <a class="topic-title" href="${pageContext.request.contextPath}/t/${topic.id}/${topic.slug}"><c:out value="${topic.title}"/></a>
                    <c:if test="${topic.unreadByCurrentUser}"><span class="mini-badge unread">${topic.unreadPostsCount} 新</span></c:if>
                    <c:if test="${topic.solved}"><span class="mini-badge ok">已解决</span></c:if>
                    <c:if test="${mode == 'top'}"><span class="mini-badge hot">热榜</span></c:if>
                </div>
                <div class="topic-meta">
                    <a class="category-badge" style="--cat-color:${topic.categoryColor}" href="${pageContext.request.contextPath}/c/${topic.categorySlug}">
                        <i data-lucide="${topic.categoryIcon}"></i><c:out value="${topic.categoryName}"/>
                    </a>
                    <c:forEach var="tagItem" items="${topic.tags}">
                        <a class="tag-badge" href="${pageContext.request.contextPath}/tag/${tagItem.slug}">#<c:out value="${tagItem.name}"/></a>
                    </c:forEach>
                </div>
            </div>
            <div class="topic-posters">
                <img class="avatar tiny" src="${topic.authorAvatarUrl}" alt="">
                <span><c:out value="${topic.authorDisplayName}"/></span>
            </div>
            <a class="num heat" href="${pageContext.request.contextPath}/t/${topic.id}">${topic.repliesCount}</a>
            <span class="num">${topic.viewsCount}</span>
            <span class="age">${topic.lastPostAgo}</span>
        </article>
    </c:forEach>
    <c:if test="${empty topicsPage.items}">
        <div class="empty-state">暂时没有符合条件的话题。</div>
    </c:if>
</section>

<c:if test="${topicsPage.totalPages > 1}">
    <nav class="pager">
        <c:if test="${topicsPage.hasPrevious}">
            <a href="?page=${topicsPage.page - 1}">上一页</a>
        </c:if>
        <span>第 ${topicsPage.page} / ${topicsPage.totalPages} 页</span>
        <c:if test="${topicsPage.hasNext}">
            <a href="?page=${topicsPage.page + 1}">下一页</a>
        </c:if>
    </nav>
</c:if>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
