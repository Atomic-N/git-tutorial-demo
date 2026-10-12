<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="page-title-row">
    <div>
        <h1>类别</h1>
        <p class="muted">按主题组织 AI 技术讨论，快速进入你关心的方向。</p>
    </div>
    <c:if test="${not empty currentUser}">
        <a class="btn btn-muted" href="${pageContext.request.contextPath}/topics/new"><i data-lucide="square-pen"></i>新建话题</a>
    </c:if>
</section>

<section class="category-layout">
    <div class="category-list">
        <c:forEach var="cat" items="${categories}">
            <a class="category-row" href="${pageContext.request.contextPath}/c/${cat.slug}" style="--cat-color:${cat.color}">
                <div class="category-icon"><i data-lucide="${cat.icon}"></i></div>
                <div class="category-copy">
                    <h2><c:out value="${cat.name}"/></h2>
                    <p><c:out value="${cat.description}"/></p>
                    <div class="category-meta">${cat.topicCount} 话题 · ${cat.postCount} 回复</div>
                </div>
                <i data-lucide="chevron-right"></i>
            </a>
        </c:forEach>
    </div>
    <aside class="latest-side">
        <h2>最新</h2>
        <c:forEach var="topic" items="${latestTopics}">
            <a class="mini-topic" href="${pageContext.request.contextPath}/t/${topic.id}">
                <img class="avatar tiny" src="${topic.authorAvatarUrl}" alt="">
                <span><c:out value="${topic.title}"/></span>
                <b>${topic.repliesCount}</b>
            </a>
        </c:forEach>
    </aside>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
