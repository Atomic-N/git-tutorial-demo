<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>

<section class="admin-hero">
    <div class="admin-hero-main">
        <div class="page-title-row admin-hero-title">
            <div>
                <span class="admin-eyebrow">Operations Console</span>
                <h1>管理后台</h1>
                <p class="muted">集中查看社区状态、处理内容、维护用户权限与站点配置。</p>
            </div>
        </div>
        <div class="admin-hero-stats">
            <article class="admin-kpi primary">
                <span>待处理举报</span>
                <strong>${stats.pendingReportsCount}</strong>
                <small>需要优先审核的内容</small>
            </article>
            <article class="admin-kpi">
                <span>今日话题</span>
                <strong>${stats.topicsTodayCount}</strong>
                <small>${stats.postsTodayCount} 条新回复</small>
            </article>
            <article class="admin-kpi">
                <span>今日活跃</span>
                <strong>${stats.activeTodayUsersCount}</strong>
                <small>${stats.newTodayUsersCount} 位新用户</small>
            </article>
        </div>
    </div>
    <aside class="admin-hero-side">
        <div class="admin-quick-actions">
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/admin/reports"><i data-lucide="flag"></i>处理举报</a>
            <a class="btn btn-ghost" href="${pageContext.request.contextPath}/admin/badges"><i data-lucide="award"></i>徽章管理</a>
            <a class="btn btn-ghost" href="${pageContext.request.contextPath}/admin/settings"><i data-lucide="sliders-horizontal"></i>站点配置</a>
            <a class="btn btn-ghost" href="${pageContext.request.contextPath}/admin/audit"><i data-lucide="scroll-text"></i>审计日志</a>
            <form action="${pageContext.request.contextPath}/admin/content/refresh-badges" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="returnTo" value="${fn:escapeXml(currentDashboardUrl)}">
                <button class="btn btn-ghost" type="submit"><i data-lucide="refresh-cw"></i>刷新徽章</button>
            </form>
        </div>
        <div class="admin-system-pulse">
            <div>
                <span>管理员 / 版主</span>
                <strong>${stats.adminUsersCount} / ${stats.moderatorUsersCount}</strong>
            </div>
            <div>
                <span>冻结账号</span>
                <strong>${stats.suspendedUsersCount}</strong>
            </div>
            <div>
                <span>审计日志</span>
                <strong>${stats.auditLogsCount}</strong>
            </div>
        </div>
    </aside>
</section>

<c:if test="${not empty param.error}">
    <div class="form-error"><c:out value="${param.error}"/></div>
</c:if>
<c:if test="${not empty param.message}">
    <div class="form-success"><c:out value="${param.message}"/></div>
</c:if>

<nav class="admin-section-nav">
    <a href="#content-governance"><i data-lucide="layout-list"></i>内容治理</a>
    <a href="#user-governance"><i data-lucide="users"></i>用户治理</a>
    <a href="${pageContext.request.contextPath}/admin/reports"><i data-lucide="flag"></i>举报</a>
    <a href="${pageContext.request.contextPath}/admin/badges"><i data-lucide="award"></i>徽章</a>
    <a href="${pageContext.request.contextPath}/admin/settings"><i data-lucide="settings"></i>配置</a>
    <a href="${pageContext.request.contextPath}/admin/audit"><i data-lucide="history"></i>审计</a>
</nav>

<section class="admin-stats admin-overview-grid">
    <div><strong>${stats.usersCount}</strong><span>注册用户</span></div>
    <div><strong>${stats.topicsCount}</strong><span>累计话题</span></div>
    <div><strong>${stats.postsCount}</strong><span>累计回复</span></div>
    <div><strong>${stats.pendingReportsCount}</strong><span>待处理举报</span></div>
    <div><strong>${stats.channelsCount}</strong><span>聊天频道</span></div>
    <div><strong>${stats.messagesCount}</strong><span>聊天消息</span></div>
    <div><strong>${stats.directMessageThreadsCount}</strong><span>私信会话</span></div>
    <div><strong>${stats.suspendedUsersCount}</strong><span>冻结账号</span></div>
</section>

<section class="admin-panel" id="content-governance">
    <div class="admin-panel-head">
        <div>
            <h2>内容治理</h2>
            <p class="muted">共匹配 ${topicsPage.total} 个话题，当前第 ${topicsPage.page} / ${topicsPage.totalPages} 页。</p>
        </div>
        <a class="btn btn-ghost" href="${pageContext.request.contextPath}/latest"><i data-lucide="external-link"></i>查看前台</a>
    </div>

    <form class="admin-filter-bar" action="${pageContext.request.contextPath}/admin/dashboard#content-governance" method="get">
        <label>
            <span>关键词</span>
            <input type="search" name="topicKeyword" value="${fn:escapeXml(topicKeyword)}" placeholder="标题或摘要">
        </label>
        <label>
            <span>分类</span>
            <select name="topicCategoryId">
                <option value="">全部分类</option>
                <c:forEach var="cat" items="${categories}">
                    <option value="${cat.id}" ${topicCategoryId == cat.id ? 'selected' : ''}><c:out value="${cat.name}"/></option>
                </c:forEach>
            </select>
        </label>
        <label>
            <span>排序</span>
            <select name="topicOrder">
                <option value="LATEST" ${topicOrder == 'LATEST' ? 'selected' : ''}>最近活跃</option>
                <option value="NEW" ${topicOrder == 'NEW' ? 'selected' : ''}>最新创建</option>
                <option value="TOP" ${topicOrder == 'TOP' ? 'selected' : ''}>热度优先</option>
                <option value="VIEWS" ${topicOrder == 'VIEWS' ? 'selected' : ''}>浏览最多</option>
                <option value="REPLIES" ${topicOrder == 'REPLIES' ? 'selected' : ''}>回复最多</option>
            </select>
        </label>
        <label>
            <span>时间</span>
            <select name="topicAgeDays">
                <option value="">不限时间</option>
                <option value="1" ${topicAgeDays == 1 ? 'selected' : ''}>今天</option>
                <option value="7" ${topicAgeDays == 7 ? 'selected' : ''}>7 天内</option>
                <option value="30" ${topicAgeDays == 30 ? 'selected' : ''}>30 天内</option>
            </select>
        </label>
        <label>
            <span>最低回复</span>
            <input type="number" name="topicMinReplies" value="${topicMinReplies}" min="0" step="1">
        </label>
        <label>
            <span>最低浏览</span>
            <input type="number" name="topicMinViews" value="${topicMinViews}" min="0" step="1">
        </label>
        <div class="admin-filter-actions">
            <button class="btn btn-primary" type="submit"><i data-lucide="filter"></i>筛选</button>
            <a class="btn btn-ghost" href="${pageContext.request.contextPath}/admin/dashboard#content-governance"><i data-lucide="rotate-ccw"></i>重置</a>
        </div>
    </form>

    <form id="bulkTopicsForm" action="${pageContext.request.contextPath}/admin/content/bulk-topics" method="post">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <input type="hidden" name="returnTo" value="${fn:escapeXml(currentDashboardUrl)}#content-governance">
    </form>
    <div class="admin-bulk-toolbar">
        <label class="switch-row inline-role">
            <input type="checkbox" data-admin-select-all="topicIds">
            <span>全选本页</span>
        </label>
        <select name="bulkAction" form="bulkTopicsForm">
            <option value="pin">批量置顶</option>
            <option value="unpin">取消置顶</option>
            <option value="delete">批量删除</option>
        </select>
        <button class="btn btn-ghost" type="submit" form="bulkTopicsForm" data-admin-bulk-submit disabled><i data-lucide="check-square"></i>执行</button>
    </div>

    <div class="admin-topic-list">
        <c:forEach var="topic" items="${topicsPage.items}">
            <article class="admin-topic-card">
                <div class="admin-topic-main">
                    <div class="admin-topic-badges">
                        <label class="admin-select-item">
                            <input type="checkbox" name="topicIds" value="${topic.id}" form="bulkTopicsForm" data-admin-select-item="topicIds">
                        </label>
                        <span class="category-badge" style="--cat-color:${topic.categoryColor}">
                            <i data-lucide="${topic.categoryIcon}"></i>
                            <c:out value="${topic.categoryName}"/>
                        </span>
                        <c:if test="${topic.pinned}">
                            <span class="mini-badge hot"><i data-lucide="pin"></i>置顶</span>
                        </c:if>
                        <c:if test="${topic.solved}">
                            <span class="mini-badge ok"><i data-lucide="badge-check"></i>已解决</span>
                        </c:if>
                    </div>
                    <h3><a href="${pageContext.request.contextPath}/t/${topic.id}"><c:out value="${topic.title}"/></a></h3>
                    <p class="muted"><c:out value="${topic.excerpt}"/></p>
                    <div class="admin-topic-meta muted">
                        <span>@<c:out value="${topic.authorUsername}"/></span>
                        <span>${topic.createdAgo}前创建</span>
                        <span>${topic.lastPostAgo}前活跃</span>
                    </div>
                </div>
                <div class="admin-topic-side">
                    <div class="admin-topic-metrics">
                        <div><strong>${topic.repliesCount}</strong><span>回复</span></div>
                        <div><strong>${topic.viewsCount}</strong><span>浏览</span></div>
                        <div><strong>${topic.likesCount}</strong><span>赞</span></div>
                    </div>
                    <div class="admin-actions-inline compact">
                        <a class="btn btn-ghost" href="${pageContext.request.contextPath}/t/${topic.id}"><i data-lucide="eye"></i>查看</a>
                        <form action="${pageContext.request.contextPath}/admin/content/${topic.pinned ? 'unpin-topic' : 'pin-topic'}" method="post">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="returnTo" value="${fn:escapeXml(currentDashboardUrl)}#content-governance">
                            <input type="hidden" name="topicId" value="${topic.id}">
                            <button class="btn btn-ghost" type="submit"><i data-lucide="pin"></i>${topic.pinned ? '取消置顶' : '置顶'}</button>
                        </form>
                        <form action="${pageContext.request.contextPath}/admin/content/delete-topic" method="post" onsubmit="return confirm('确认删除该话题？')">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="returnTo" value="${fn:escapeXml(currentDashboardUrl)}#content-governance">
                            <input type="hidden" name="topicId" value="${topic.id}">
                            <button class="btn btn-danger-soft" type="submit"><i data-lucide="trash-2"></i>删除</button>
                        </form>
                    </div>
                </div>
            </article>
        </c:forEach>
        <c:if test="${empty topicsPage.items}">
            <div class="empty-state">暂无匹配话题。</div>
        </c:if>
    </div>

    <c:if test="${topicsPage.totalPages > 1}">
        <nav class="pager admin-pager">
            <c:if test="${topicsPage.hasPrevious}">
                <a href="${topicPageBaseUrl}${topicsPage.page - 1}#content-governance">上一页</a>
            </c:if>
            <span>第 ${topicsPage.page} / ${topicsPage.totalPages} 页</span>
            <c:if test="${topicsPage.hasNext}">
                <a href="${topicPageBaseUrl}${topicsPage.page + 1}#content-governance">下一页</a>
            </c:if>
        </nav>
    </c:if>
</section>

<section class="admin-panel" id="user-governance">
    <div class="admin-panel-head">
        <div>
            <h2>用户治理</h2>
            <p class="muted">共匹配 ${usersPage.total} 位用户，当前第 ${usersPage.page} / ${usersPage.totalPages} 页。</p>
        </div>
        <a class="btn btn-ghost" href="${pageContext.request.contextPath}/badges"><i data-lucide="award"></i>查看徽章体系</a>
    </div>

    <form class="admin-filter-bar user-filter" action="${pageContext.request.contextPath}/admin/dashboard#user-governance" method="get">
        <label>
            <span>用户</span>
            <input type="search" name="userKeyword" value="${fn:escapeXml(userKeyword)}" placeholder="用户名、昵称或邮箱">
        </label>
        <label>
            <span>状态</span>
            <select name="userStatus">
                <option value="ALL" ${userStatus == 'ALL' ? 'selected' : ''}>全部状态</option>
                <option value="ACTIVE" ${userStatus == 'ACTIVE' ? 'selected' : ''}>ACTIVE</option>
                <option value="SUSPENDED" ${userStatus == 'SUSPENDED' ? 'selected' : ''}>SUSPENDED</option>
            </select>
        </label>
        <label>
            <span>角色</span>
            <select name="userRole">
                <option value="ALL" ${userRole == 'ALL' ? 'selected' : ''}>全部角色</option>
                <c:forEach var="role" items="${roles}">
                    <option value="${role.code}" ${userRole == role.code ? 'selected' : ''}><c:out value="${role.name}"/></option>
                </c:forEach>
            </select>
        </label>
        <label>
            <span>信任等级</span>
            <select name="userTrustLevel">
                <option value="">全部等级</option>
                <c:forEach var="i" begin="0" end="4">
                    <option value="${i}" ${userTrustLevel == i ? 'selected' : ''}>Lv${i}</option>
                </c:forEach>
            </select>
        </label>
        <div class="admin-filter-actions">
            <button class="btn btn-primary" type="submit"><i data-lucide="search"></i>查找</button>
            <a class="btn btn-ghost" href="${pageContext.request.contextPath}/admin/dashboard#user-governance"><i data-lucide="rotate-ccw"></i>重置</a>
        </div>
    </form>

    <div class="admin-user-list">
        <c:forEach var="user" items="${usersPage.items}">
            <form class="admin-user-card" action="${pageContext.request.contextPath}/admin/content/update-user" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="returnTo" value="${fn:escapeXml(currentDashboardUrl)}#user-governance">
                <input type="hidden" name="userId" value="${user.id}">
                <div class="admin-user-identity">
                    <img src="${user.avatarUrl}" alt="">
                    <div>
                        <strong><c:out value="${user.displayName}"/></strong>
                        <div class="muted">@<c:out value="${user.username}"/> · <c:out value="${user.email}"/></div>
                        <div class="admin-user-badges">
                            <c:if test="${user.admin}">
                                <span class="role-badge admin"><i data-lucide="shield-check"></i>管理员</span>
                            </c:if>
                            <c:if test="${user.scopedModerator}">
                                <span class="role-badge moderator"><i data-lucide="shield"></i>版主</span>
                            </c:if>
                        </div>
                        <div class="admin-user-summary muted">
                            <span>${user.joinedAgo}前加入</span>
                            <span>${user.topicsCount} 话题</span>
                            <span>${user.postsCount} 回复</span>
                            <span>${user.likesReceivedCount} 获赞</span>
                        </div>
                    </div>
                </div>
                <div class="admin-user-controls">
                    <label>
                        <span class="muted">信任等级</span>
                        <select name="trustLevel">
                            <c:forEach var="i" begin="0" end="4">
                                <option value="${i}" ${user.trustLevel == i ? 'selected' : ''}>Lv${i}</option>
                            </c:forEach>
                        </select>
                    </label>
                    <label>
                        <span class="muted">账号状态</span>
                        <select name="status">
                            <option value="ACTIVE" ${user.status == 'ACTIVE' ? 'selected' : ''}>ACTIVE</option>
                            <option value="SUSPENDED" ${user.status == 'SUSPENDED' ? 'selected' : ''}>SUSPENDED</option>
                        </select>
                    </label>
                    <div class="admin-role-group">
                        <span class="muted">角色权限</span>
                        <div class="admin-role-list">
                            <c:forEach var="role" items="${roles}">
                                <label class="switch-row inline-role">
                                    <input type="checkbox" name="roles" value="${role.code}" ${user.hasRole(role.code) ? 'checked' : ''}>
                                    <span><c:out value="${role.name}"/></span>
                                </label>
                            </c:forEach>
                        </div>
                        <span class="muted">负责板块</span>
                        <div class="admin-category-scope">
                            <c:forEach var="cat" items="${categories}">
                                <label class="switch-row inline-role">
                                    <input type="checkbox" name="moderatedCategories" value="${cat.id}" ${user.moderatesCategory(cat.id) ? 'checked' : ''}>
                                    <span><c:out value="${cat.name}"/></span>
                                </label>
                            </c:forEach>
                        </div>
                        <small class="muted">仅版主角色生效，管理员默认全站。</small>
                    </div>
                </div>
                <div class="admin-user-actions">
                    <c:choose>
                        <c:when test="${user.status == 'ACTIVE'}">
                            <span class="mini-badge ok">正常</span>
                        </c:when>
                        <c:otherwise>
                            <span class="mini-badge hot">已冻结</span>
                        </c:otherwise>
                    </c:choose>
                    <a class="btn btn-ghost" href="${pageContext.request.contextPath}/u/${user.username}"><i data-lucide="user-round"></i>主页</a>
                    <button class="btn btn-primary" type="submit"><i data-lucide="save"></i>保存</button>
                </div>
            </form>
        </c:forEach>
        <c:if test="${empty usersPage.items}">
            <div class="empty-state">暂无匹配用户。</div>
        </c:if>
    </div>

    <c:if test="${usersPage.totalPages > 1}">
        <nav class="pager admin-pager">
            <c:if test="${usersPage.hasPrevious}">
                <a href="${userPageBaseUrl}${usersPage.page - 1}#user-governance">上一页</a>
            </c:if>
            <span>第 ${usersPage.page} / ${usersPage.totalPages} 页</span>
            <c:if test="${usersPage.hasNext}">
                <a href="${userPageBaseUrl}${usersPage.page + 1}#user-governance">下一页</a>
            </c:if>
        </nav>
    </c:if>
</section>

<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
