<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="search-page">
    <h1>搜索</h1>
    <form class="search-form" action="${pageContext.request.contextPath}/search" method="get">
        <div class="search-main-line">
            <input name="q" value="${criteria.keyword}" placeholder="搜索">
            <button class="btn btn-primary" type="submit"><i data-lucide="search"></i>搜索</button>
        </div>
        <div class="advanced-box">
            <label>分类
                <select name="categoryId">
                    <option value="">所有类别</option>
                    <c:forEach var="cat" items="${categories}">
                        <option value="${cat.id}" ${criteria.categoryId == cat.id ? 'selected' : ''}><c:out value="${cat.name}"/></option>
                    </c:forEach>
                </select>
            </label>
            <label>拥有该标签
                <select name="tagId">
                    <option value="">选择...</option>
                    <c:forEach var="tagItem" items="${tags}">
                        <option value="${tagItem.id}" ${criteria.tagId == tagItem.id ? 'selected' : ''}><c:out value="${tagItem.name}"/></option>
                    </c:forEach>
                </select>
            </label>
            <label>发帖人
                <input name="author" value="${criteria.author}" placeholder="用户名或昵称">
            </label>
            <label>发布时间
                <select name="days">
                    <option value="">不限</option>
                    <option value="1" ${criteria.createdWithinDays == 1 ? 'selected' : ''}>1 天内</option>
                    <option value="7" ${criteria.createdWithinDays == 7 ? 'selected' : ''}>7 天内</option>
                    <option value="30" ${criteria.createdWithinDays == 30 ? 'selected' : ''}>30 天内</option>
                    <option value="90" ${criteria.createdWithinDays == 90 ? 'selected' : ''}>90 天内</option>
                </select>
            </label>
            <label>最少回复
                <input name="minReplies" type="number" min="0" value="${criteria.minReplies}">
            </label>
            <label>最多回复
                <input name="maxReplies" type="number" min="0" value="${criteria.maxReplies}">
            </label>
            <label>最少浏览
                <input name="minViews" type="number" min="0" value="${criteria.minViews}">
            </label>
            <label>最多浏览
                <input name="maxViews" type="number" min="0" value="${criteria.maxViews}">
            </label>
            <label>排序方式
                <select name="orderBy">
                    <option value="LATEST" ${criteria.orderBy == 'LATEST' ? 'selected' : ''}>最新活动</option>
                    <option value="NEW" ${criteria.orderBy == 'NEW' ? 'selected' : ''}>最新发布</option>
                    <option value="TOP" ${criteria.orderBy == 'TOP' ? 'selected' : ''}>热度优先</option>
                    <option value="REPLIES" ${criteria.orderBy == 'REPLIES' ? 'selected' : ''}>回复最多</option>
                    <option value="VIEWS" ${criteria.orderBy == 'VIEWS' ? 'selected' : ''}>浏览最多</option>
                </select>
            </label>
            <label class="switch-row inline-switch"><input type="checkbox" name="solvedOnly" ${criteria.solvedOnly ? 'checked' : ''}>只看已解决</label>
        </div>
    </form>
</section>

<section class="topic-table search-results">
    <div class="topic-head">
        <span>搜索结果</span><span>回复</span><span>浏览量</span><span>活动</span>
    </div>
    <c:forEach var="topic" items="${topicsPage.items}">
        <article class="topic-row ${topic.unreadByCurrentUser ? 'unread-topic' : ''}">
            <div class="topic-main">
                <a class="topic-title" href="${pageContext.request.contextPath}/t/${topic.id}/${topic.slug}"><c:out value="${topic.title}"/></a>
                <p class="topic-excerpt"><c:out value="${topic.excerpt}"/></p>
                <div class="topic-meta">
                    <a class="category-badge" style="--cat-color:${topic.categoryColor}" href="${pageContext.request.contextPath}/c/${topic.categorySlug}">
                        <i data-lucide="${topic.categoryIcon}"></i><c:out value="${topic.categoryName}"/>
                    </a>
                    <c:if test="${topic.solved}"><span class="mini-badge ok">已解决</span></c:if>
                    <c:forEach var="tagItem" items="${topic.tags}">
                        <a class="tag-badge" href="${pageContext.request.contextPath}/tag/${tagItem.slug}">#<c:out value="${tagItem.name}"/></a>
                    </c:forEach>
                </div>
            </div>
            <div class="topic-posters"><img class="avatar tiny" src="${topic.authorAvatarUrl}" alt=""><span><c:out value="${topic.authorDisplayName}"/></span></div>
            <span class="num heat">${topic.repliesCount}</span>
            <span class="num">${topic.viewsCount}</span>
            <span class="age">${topic.lastPostAgo}</span>
        </article>
    </c:forEach>
    <c:if test="${empty topicsPage.items}">
        <div class="empty-state">没有找到匹配内容。</div>
    </c:if>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
