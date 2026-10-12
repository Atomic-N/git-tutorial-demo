<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="topic-detail-head">
    <div>
        <h1><c:out value="${detail.topic.title}"/></h1>
        <div class="topic-meta">
            <a class="category-badge" style="--cat-color:${detail.topic.categoryColor}" href="${pageContext.request.contextPath}/c/${detail.topic.categorySlug}">
                <i data-lucide="${detail.topic.categoryIcon}"></i><c:out value="${detail.topic.categoryName}"/>
            </a>
            <c:forEach var="tagItem" items="${detail.tags}">
                <a class="tag-badge" href="${pageContext.request.contextPath}/tag/${tagItem.slug}">#<c:out value="${tagItem.name}"/></a>
            </c:forEach>
            <span>${detail.topic.viewsCount} 浏览</span>
        </div>
    </div>
    <div class="detail-actions">
        <c:if test="${not empty currentUser && (currentUser.canModerateCategory(detail.topic.categoryId) || currentUser.id == detail.topic.userId)}">
            <a class="btn btn-ghost" href="${pageContext.request.contextPath}/topics/edit?topicId=${detail.topic.id}">
                <i data-lucide="square-pen"></i>编辑
            </a>
            <form class="inline-form" action="${pageContext.request.contextPath}/topics/delete" method="post" onsubmit="return confirm('确认删除这个话题？')">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="topicId" value="${detail.topic.id}">
                <button class="btn btn-danger" type="submit"><i data-lucide="trash-2"></i>删除</button>
            </form>
        </c:if>
        <button class="btn btn-muted ajax-toggle ${detail.topic.likedByCurrentUser ? 'active' : ''}" data-action="topic-like" data-id="${detail.topic.id}" data-sync-key="topic-like-${detail.topic.id}">
            <i data-lucide="heart"></i><span data-count data-sync-count="topic-like-${detail.topic.id}">${detail.topic.likesCount}</span>
        </button>
        <button class="btn btn-muted ajax-toggle ${detail.topic.bookmarkedByCurrentUser ? 'active' : ''}" data-action="topic-bookmark" data-id="${detail.topic.id}">
            <i data-lucide="bookmark"></i><span data-count>${detail.topic.bookmarksCount}</span>
        </button>
    </div>
</section>

<c:if test="${not empty param.error}">
    <div class="form-error"><c:out value="${param.error}"/></div>
</c:if>
<c:if test="${not empty param.message}">
    <div class="form-success"><c:out value="${param.message}"/></div>
</c:if>

<section class="post-stream">
    <c:forEach var="post" items="${detail.posts}">
        <c:choose>
            <c:when test="${post.postNumber == 1}">
                <article class="post-item hero-post" id="post-${post.id}">
                    <a class="avatar-link hero-avatar-link" href="${pageContext.request.contextPath}/u/${post.authorUsername}">
                        <img class="avatar hero-avatar" src="${post.authorAvatarUrl}" alt="">
                    </a>
                    <div class="post-body hero-post-body">
                        <div class="hero-post-main">
                            <div class="post-info hero-post-info">
                                <a href="${pageContext.request.contextPath}/u/${post.authorUsername}"><strong><c:out value="${post.authorDisplayName}"/></strong></a>
                                <span>@<c:out value="${post.authorUsername}"/></span>
                                <c:choose>
                                    <c:when test="${post.authorAdmin}">
                                        <span class="role-badge admin"><i data-lucide="shield-check"></i>管理员</span>
                                    </c:when>
                                    <c:when test="${post.authorModerator}">
                                        <span class="role-badge moderator"><i data-lucide="shield"></i>版主</span>
                                    </c:when>
                                </c:choose>
                                <c:if test="${post.userId == detail.topic.userId}">
                                    <span class="post-owner-badge">楼主</span>
                                </c:if>
                            </div>
                            <div class="cooked-content hero-post-content">${post.cookedContent}</div>
                            <div class="hero-post-footer">
                                <div class="hero-post-summary">
                                    <button class="text-button ajax-toggle reaction-chip ${detail.topic.likedByCurrentUser ? 'active' : ''}" data-action="topic-like" data-id="${detail.topic.id}" data-sync-key="topic-like-${detail.topic.id}">
                                        <i data-lucide="heart"></i><span data-count data-sync-count="topic-like-${detail.topic.id}">${detail.topic.likesCount}</span>
                                    </button>
                                    <span class="hero-stat-inline">${detail.topic.viewsCount} 浏览</span>
                                    <span class="hero-stat-inline">${detail.topic.repliesCount} 回复</span>
                                    <span class="hero-stat-inline">${detail.topic.bookmarksCount} 收藏</span>
                                </div>
                                <div class="post-actions hero-post-actions">
                                    <c:if test="${not empty currentUser}">
                                        <button class="text-button quote-button" type="button" data-post="${post.id}" data-author="@${post.authorUsername}"><i data-lucide="reply"></i>回复</button>
                                        <details class="report-menu">
                                            <summary><i data-lucide="flag"></i>举报</summary>
                                            <form action="${pageContext.request.contextPath}/reports/create" method="post">
                                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                                <input type="hidden" name="targetType" value="TOPIC">
                                                <input type="hidden" name="targetId" value="${detail.topic.id}">
                                                <input type="hidden" name="back" value="${pageContext.request.contextPath}/t/${detail.topic.id}">
                                                <select name="reason">
                                                    <option value="垃圾信息">垃圾信息</option>
                                                    <option value="不当言论">不当言论</option>
                                                    <option value="偏离话题">偏离话题</option>
                                                    <option value="违规推广">违规推广</option>
                                                </select>
                                                <input name="detail" placeholder="补充说明">
                                                <button type="submit">提交</button>
                                            </form>
                                        </details>
                                    </c:if>
                                    <c:if test="${not empty currentUser && (currentUser.canModerateCategory(detail.topic.categoryId) || currentUser.id == detail.topic.userId)}">
                                        <a class="text-button" href="${pageContext.request.contextPath}/topics/edit?topicId=${detail.topic.id}"><i data-lucide="square-pen"></i>编辑</a>
                                    </c:if>
                                </div>
                            </div>
                            <div class="hero-topic-stats">
                                <div><strong>${detail.topic.viewsCount}</strong><span>浏览量</span></div>
                                <div><strong data-sync-count="topic-like-${detail.topic.id}">${detail.topic.likesCount}</strong><span>赞</span></div>
                                <div><strong>${detail.topic.bookmarksCount}</strong><span>收藏</span></div>
                                <div><strong>${detail.topic.repliesCount}</strong><span>回复</span></div>
                            </div>
                        </div>
                        <aside class="hero-post-side">
                            <div class="hero-side-topline">
                                <a class="hero-side-index" href="#post-${post.id}">#${post.postNumber}</a>
                                <span class="hero-side-time">${post.createdAgo}</span>
                            </div>
                            <div class="hero-side-date">${post.createdAt.monthValue}月 ${post.createdAt.dayOfMonth}日</div>
                            <div class="hero-side-rail">
                                <span class="hero-side-rail-line"></span>
                                <span class="hero-side-rail-badge">${post.postNumber} / ${detail.posts.size()}</span>
                            </div>
                            <div class="post-owner-note">TOPIC OWNER</div>
                        </aside>
                    </div>
                </article>
            </c:when>
            <c:otherwise>
                <article class="post-item reply-post ${post.userId == detail.topic.userId ? 'topic-owner-reply' : ''}" id="post-${post.id}">
                    <a class="avatar-link" href="${pageContext.request.contextPath}/u/${post.authorUsername}">
                        <img class="avatar" src="${post.authorAvatarUrl}" alt="">
                    </a>
                    <div class="post-body reply-post-body">
                        <div class="reply-post-head">
                            <div class="post-info reply-post-info">
                                <a href="${pageContext.request.contextPath}/u/${post.authorUsername}"><strong><c:out value="${post.authorDisplayName}"/></strong></a>
                                <span>@<c:out value="${post.authorUsername}"/></span>
                                <c:choose>
                                    <c:when test="${post.authorAdmin}">
                                        <span class="role-badge admin"><i data-lucide="shield-check"></i>管理员</span>
                                    </c:when>
                                    <c:when test="${post.authorModerator}">
                                        <span class="role-badge moderator"><i data-lucide="shield"></i>版主</span>
                                    </c:when>
                                </c:choose>
                                <c:if test="${post.userId == detail.topic.userId}">
                                    <span class="post-owner-badge">楼主</span>
                                </c:if>
                            </div>
                            <div class="reply-post-meta">
                                <span>${post.createdAgo}</span>
                                <a class="post-number" href="#post-${post.id}">#${post.postNumber}</a>
                            </div>
                        </div>
                        <c:if test="${not empty post.replyToPostId}">
                            <c:forEach var="replyTarget" items="${detail.posts}">
                                <c:if test="${replyTarget.id == post.replyToPostId}">
                                    <a class="reply-context" href="#post-${post.replyToPostId}">
                                        <span class="reply-context-label">回复</span>
                                        <strong>@<c:out value="${replyTarget.authorUsername}"/></strong>
                                        <span>#<c:out value="${replyTarget.postNumber}"/></span>
                                        <c:if test="${not empty replyTarget.rawContent}">
                                            <em><c:out value="${replyTarget.rawContent}"/></em>
                                        </c:if>
                                    </a>
                                </c:if>
                            </c:forEach>
                        </c:if>
                        <div class="cooked-content reply-post-content">${post.cookedContent}</div>
                        <div class="post-actions reply-post-actions">
                            <button class="text-button ajax-toggle ${post.likedByCurrentUser ? 'active' : ''}" data-action="post-like" data-id="${post.id}">
                                <i data-lucide="heart"></i><span data-count>${post.likesCount}</span>
                            </button>
                            <c:if test="${post.solution}">
                                <span class="mini-badge ok"><i data-lucide="badge-check"></i>最佳答案</span>
                            </c:if>
                            <c:if test="${not empty currentUser}">
                                <button class="text-button quote-button" type="button" data-post="${post.id}" data-author="@${post.authorUsername}"><i data-lucide="reply"></i>回复</button>
                                <details class="report-menu">
                                    <summary><i data-lucide="flag"></i>举报</summary>
                                    <form action="${pageContext.request.contextPath}/reports/create" method="post">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="targetType" value="POST">
                                        <input type="hidden" name="targetId" value="${post.id}">
                                        <input type="hidden" name="back" value="${pageContext.request.contextPath}/t/${detail.topic.id}">
                                        <select name="reason">
                                            <option value="垃圾信息">垃圾信息</option>
                                            <option value="不当言论">不当言论</option>
                                            <option value="偏离话题">偏离话题</option>
                                            <option value="违规推广">违规推广</option>
                                        </select>
                                        <input name="detail" placeholder="补充说明">
                                        <button type="submit">提交</button>
                                    </form>
                                </details>
                            </c:if>
                            <c:if test="${not empty currentUser && (currentUser.canModerateCategory(detail.topic.categoryId) || currentUser.id == detail.topic.userId)}">
                                <form class="inline-form" action="${pageContext.request.contextPath}/topics/solution" method="post">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="topicId" value="${detail.topic.id}">
                                    <input type="hidden" name="postId" value="${post.id}">
                                    <c:if test="${post.solution}"><input type="hidden" name="clear" value="1"></c:if>
                                    <button class="text-button ${post.solution ? 'active' : ''}" type="submit"><i data-lucide="badge-check"></i>${post.solution ? '取消最佳答案' : '设为最佳答案'}</button>
                                </form>
                            </c:if>
                            <c:if test="${not empty currentUser && (currentUser.canModerateCategory(detail.topic.categoryId) || currentUser.id == post.userId)}">
                                <a class="text-button" href="${pageContext.request.contextPath}/posts/edit?postId=${post.id}"><i data-lucide="square-pen"></i>编辑</a>
                                <form class="inline-form" action="${pageContext.request.contextPath}/posts/delete" method="post" onsubmit="return confirm('确认删除这条回复？')">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="topicId" value="${detail.topic.id}">
                                    <input type="hidden" name="postId" value="${post.id}">
                                    <button class="text-button danger" type="submit"><i data-lucide="trash-2"></i>删除</button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                </article>
            </c:otherwise>
        </c:choose>
    </c:forEach>
</section>

<section id="reply-box" class="reply-box">
    <c:choose>
        <c:when test="${not empty currentUser}">
            <h2>回复</h2>
            <form class="draft-form" action="${pageContext.request.contextPath}/posts/create" method="post" data-draft-type="REPLY" data-draft-target="${detail.topic.id}">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="topicId" value="${detail.topic.id}">
                <input type="hidden" name="replyToPostId" id="replyToPostId">
                <div class="reply-target-banner" id="replyTargetBanner" hidden>
                    <span>正在回复 <strong id="replyTargetName"></strong></span>
                    <button class="text-button" id="replyTargetClear" type="button"><i data-lucide="x"></i>取消</button>
                </div>
                <textarea name="content" data-draft-content rows="6" placeholder="写下你的回复" required></textarea>
                <div class="editor-tools">
                    <label class="upload-tool"><i data-lucide="paperclip"></i>上传附件
                        <input class="file-upload-input" type="file" data-insert-target="#reply-box textarea">
                    </label>
                    <span class="upload-status" data-upload-status></span>
                    <span class="draft-status" data-draft-status>草稿会自动保存</span>
                </div>
                <div class="form-actions">
                    <button class="btn btn-primary" type="submit"><i data-lucide="send"></i>发布回复</button>
                </div>
            </form>
        </c:when>
        <c:otherwise>
            <div class="empty-state">登录后即可参与回复。<a href="${pageContext.request.contextPath}/login">去登录</a></div>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
