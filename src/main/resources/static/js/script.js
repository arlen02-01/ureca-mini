document.addEventListener("DOMContentLoaded", () => {
  // -------------------------
  // DOM Helpers
  // -------------------------
  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => Array.from(document.querySelectorAll(sel));

  const contentArea = $("#contentArea");
  const detailApplyBtn = $("#detailApplyBtn");


  // Home tabs
  const homeTabs = {
    team: $("#teamBox"),
    board: $("#boardBox"),
  };

  // Recruit create modal
  const recruitModal = $("#recruitModal");
  const recruitForm = $("#recruitForm");
  const closeRecruitBtn = $("#closeRecruitBtn");

  // Detail modal (home에만 있어도 됨)
  const detailModal = $("#detailModal");
  const detailCloseBtn = $("#detailCloseBtn");
  const detailCompleteBtn = $("#detailCompleteBtn");

  const detailTitle = $("#detailTitle");
  const detailCreator = $("#detailCreator");
  const detailSpots = $("#detailSpots");
  const detailStatus = $("#detailStatus");
  const detailCountdown = $("#detailCountdown");
  const detailDesc = $("#detailDesc");

  const isHome = !!(homeTabs.team && homeTabs.board);
  const isMyPage = !!($("#teamTab") && $("#friendsTab"));

  // -------------------------
  // State
  // -------------------------
  const state = {
    home: { page: 0, size: 10 },
    my: { page: 0, size: 5 },
    applied: { page: 0, size: 5 },
    countdownTimer: null,
	selectedFriendIds: new Set(),
	selectedFriendNames: new Map(),
    view: "myPosts", // "homeTeam" | "myPosts" | "myApplied"
  };


  // -------------------------
  // CSRF Helpers
  // -------------------------
  function getCsrf() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    if (!token || !header) return null;
    return { token, header };
  }

  function withCsrfHeaders(opts = {}) {
    const csrf = getCsrf();
    if (!csrf) return opts;

    const headers = new Headers(opts.headers || {});
    headers.set(csrf.header, csrf.token);
    return { ...opts, headers };
  }

  // -------------------------
  // UI Utils
  // -------------------------
  function setLoading() {
    if (!contentArea) return;
    contentArea.innerHTML = "<p>불러오는 중...</p>";
  }

  function setError(msg = "데이터를 불러오는데 실패했습니다.") {
    if (!contentArea) return;
    contentArea.innerHTML = `<p>${msg}</p>`;
  }

  function setActiveTab(tabA, tabB, activeEl) {
    if (!tabA || !tabB || !activeEl) return;
    [tabA, tabB].forEach((el) => {
      el.classList.add("tab-btn");
      el.classList.toggle("active-tab", el === activeEl);
    });
  }

  // -------------------------
  // API
  // -------------------------
  async function fetchJson(url, opts = {}) {
    const merged = withCsrfHeaders({
      credentials: "same-origin",
      ...opts,
    });

    const res = await fetch(url, merged);

    const ct = (res.headers.get("content-type") || "").toLowerCase();

    // 먼저 text로 읽어서(HTML이면 여기서 잡힘) 필요하면 JSON 파싱
    const rawText = await res.text();

    // ❗️HTML(로그인 페이지/에러 페이지/thymeleaf)이면 바로 원인 보여주기
    const looksLikeHtml = ct.includes("text/html") || rawText.trim().startsWith("<!doctype") || rawText.trim().startsWith("<html");
    if (looksLikeHtml) {
      console.error("❗️서버가 JSON 대신 HTML을 반환했습니다. (대부분 로그인 리다이렉트/권한/https 문제)", {
        url,
        status: res.status,
        contentType: ct,
        preview: rawText.slice(0, 200),
      });
      throw new Error("HTML response (not JSON). Check login/authority/http-https.");
    }

    let data = null;
    if (ct.includes("application/json")) {
      data = JSON.parse(rawText);
    } else {
      // content-type이 애매해도 JSON일 수 있으니 한번 시도
      try {
        data = JSON.parse(rawText);
      } catch {
        data = rawText;
      }
    }

    if (!res.ok) {
      console.error("API ERROR:", { url, status: res.status, body: data });
      throw new Error(`HTTP ${res.status}`);
    }
    return data;
  }
  // -------------------------
  // Pagination Render
  // -------------------------
  function renderPagination(pageObj) {
    const current = pageObj.number ?? 0; // 0-based
    const total = pageObj.totalPages ?? 1;
    const isFirst = !!pageObj.first;
    const isLast = !!pageObj.last;

    return `
      <div class="pagination">
        <span class="page-info">${current + 1} / ${total}</span>
        <button id="prevPageBtn" class="page-btn" ${isFirst ? "disabled" : ""}>◀</button>
        <button id="nextPageBtn" class="page-btn" ${isLast ? "disabled" : ""}>▶</button>
      </div>
    `;
  }

  // -------------------------
  // Countdown (공통)
  // -------------------------
  function startCountdown() {
    if (state.countdownTimer) {
      clearInterval(state.countdownTimer);
      state.countdownTimer = null;
    }

    const elements = $$(".countdown");
    if (elements.length === 0) return;

    const pad = (n) => String(n).padStart(2, "0");

    const update = () => {
      const now = new Date();

      elements.forEach((el) => {
        const card = el.closest(".card") || el.closest(".modal-content");
        const status = card?.getAttribute("data-status");

        if (status === "CLOSED" || status === "COMPLETED") {
          el.textContent = "마감";
          return;
        }

        const endStr = el.getAttribute("data-end-time");
        if (!endStr) {
          el.textContent = "-";
          return;
        }

        const end = new Date(endStr);
        const diff = end - now;

        if (diff <= 0) {
          el.textContent = "마감";
          // 카드라면 색 바꾸기
          const c = el.closest(".card");
          if (c) {
            c.classList.remove("open");
            c.classList.add("closed");
            c.setAttribute("data-status", "COMPLETED");
          }
          return;
        }

        const t = Math.floor(diff / 1000);
        const d = Math.floor(t / 86400);
        const h = Math.floor((t % 86400) / 3600);
        const m = Math.floor((t % 3600) / 60);
        const s = t % 60;

        el.textContent = `${d > 0 ? d + "일 " : ""}${pad(h)}시간 ${pad(m)}분 ${pad(s)}초`;
      });
    };

    update();
    state.countdownTimer = setInterval(update, 1000);
  }
  
  function hasApplied(applications) {
    if (!Array.isArray(applications)) return false;
    if (CURRENT_USER_ID == null) return false;

    return applications.some(app => Number(app.userId) === Number(CURRENT_USER_ID));
  }


  async function applyRecruit(recruitId) {
    // CSRF/credentials는 fetchJson이 알아서 넣고 있으니 그대로 사용
    return await fetchJson(`/recruitments/apply/${recruitId}`, { method: "POST" });
  }
  recruitModal?.addEventListener("click", (e) => {
    const openBtn = e.target.closest("#openFriendPickerBtn");
    if (openBtn) {
      loadFriendPicker();
      return;
    }

    const pickBtn = e.target.closest(".btn-pick-friend");
    if (pickBtn) {
      const uid = Number(pickBtn.dataset.userId);
      const nm = pickBtn.dataset.name || "";
      if (!Number.isFinite(uid)) return;

      state.selectedFriendIds.add(uid);
      state.selectedFriendNames.set(uid, nm);

      pickBtn.disabled = true;
      pickBtn.textContent = "선택됨";

      renderSelectedFriends();
      return;
    }

    const removeBtn = e.target.closest(".btn-remove-picked");
    if (removeBtn) {
      const uid = Number(removeBtn.dataset.userId);
      state.selectedFriendIds.delete(uid);
      state.selectedFriendNames.delete(uid);
      renderSelectedFriends();
      return;
    }
  });


  contentArea?.addEventListener("click", async (e) => {
	const btn = e.target.closest(".btn-apply");
	if (!btn) return;
	
    e.stopPropagation(); // 카드 상세 열기 막기
    const id = btn.dataset.id;
    if (!id) return;

    if (!confirm("이 모집에 신청하시겠습니까?")) return;

    try {
      await applyRecruit(id);

      // 신청 성공 → 리스트 새로고침(인원 수 갱신)
      refreshCurrentList();
      alert("신청이 완료되었습니다!");
    } catch (err) {
      console.error(err);

      // 백엔드가 상태코드로 구분해주면 여기서 분기 가능
      // 예: 409 = 정원초과 / 400 = 이미 신청 / 401 = 로그인 필요
      alert("신청에 실패했습니다. (정원 초과/중복 신청/로그인 필요 여부 확인)");
    }
  });

  function renderFriendsLayout() {
    contentArea.innerHTML = `
      <div class="list-header">
        <h2>친구</h2>
      </div>

      <div id="friendsView"></div>
    `;
  }

  async function loadFriendFind() {
    state.view = "friendsFind";
    renderFriendsLayout();

    const view = $("#friendsView");
    if (!view) return;

    if (CURRENT_USER_ID === null) {
      view.innerHTML = `<p>로그인 후 이용 가능합니다.</p>`;
      return;
    }

    view.innerHTML = `
      <h3>친구 찾기</h3>
      <div style="display:flex; gap:8px; width:100%; align-items:center;">
        <input id="friendNameInput" type="text" placeholder="이름 입력" style="flex:1; padding:6px 8px; border-radius:6px; border:1px solid #ccc;">
        <button id="sendFriendReqBtn" class="page-btn">전송</button>
      </div>

      <hr style="width:100%; margin:16px 0;">

      <h3>친구 추천</h3>
      <div id="recommendList"><p>불러오는 중...</p></div>
    `;

    // 전송(친구 요청)
    $("#sendFriendReqBtn")?.addEventListener("click", async () => {
      const name = $("#friendNameInput")?.value?.trim();
      if (!name) return alert("이름을 입력해주세요.");

      try {
        // ✅ /friends/request?receiverName=...
		await fetchJson(`/friends/request?receiverName=${encodeURIComponent(name)}`, {
		  method: "POST",
		});

        alert("친구 요청을 전송했습니다.");
        $("#friendNameInput").value = "";
      } catch (e) {
        console.error(e);
        alert("친구 요청 전송에 실패했습니다.");
      }
    });

    // 추천 리스트
    try {
      // ✅ 추천: GET /friends?limit=10
	  const res = await fetchJson(`/friends/recommend?limit=10`);
      const list = res?.data ?? [];

      const wrap = $("#recommendList");
      if (!wrap) return;

      if (!Array.isArray(list) || list.length === 0) {
        wrap.innerHTML = `<p class="muted">추천 친구가 없습니다.</p>`;
        return;
      }

      // FriendRecommendDto 필드명을 모르니 name 계열로 최대한 안전하게 처리
      wrap.innerHTML = `
        <div class="card-container">
          ${list.map(r => {
			const nm = r.userName;
			const common = r.commonCount; // 공통친구 수
			const userId = r.userId;
            return `
              <div class="card open" style="cursor:default;">
			  	<div class="ctitle">${nm}</div>
			  	<div class="meta">공통 친구 ${common}명</div>
                <div class="card-actions">
                  <button class="btn-send-recommend" data-name="${nm}">요청</button>
                </div>
              </div>
            `;
          }).join("")}
        </div>
      `;

      // 추천에서 바로 요청 보내기
      wrap.addEventListener("click", async (e) => {
        const btn = e.target.closest(".btn-send-recommend");
        if (!btn) return;

        const receiverName = btn.dataset.name;
        if (!receiverName) return;

        try {
          await fetchJson(`/friends/request?receiverName=${encodeURIComponent(receiverName)}`, {
            method: "POST",
          });
          alert(`${receiverName}님에게 친구 요청을 보냈습니다.`);
          btn.disabled = true;
          btn.textContent = "전송됨";
        } catch (err) {
          console.error(err);
          alert("친구 요청 전송에 실패했습니다.");
        }
      });

    } catch (e) {
      console.error(e);
      $("#recommendList").innerHTML = `<p class="muted">추천 목록을 불러오지 못했습니다.</p>`;
    }
  }

  async function loadFriendRequests() {
    state.view = "friendsRequests";
    renderFriendsLayout();

    const view = $("#friendsView");
    if (!view) return;

    if (CURRENT_USER_ID === null) {
      view.innerHTML = `<p>로그인 후 이용 가능합니다.</p>`;
      return;
    }

    view.innerHTML = `
      <h3>받은 친구 요청</h3>
      <div id="requestList"><p>불러오는 중...</p></div>
    `;

    try {
      // ✅ GET /friends/requests
      const res = await fetchJson(`/friends/requests`);
      const list = res?.data ?? [];

      const wrap = $("#requestList");
      if (!wrap) return;

      if (!Array.isArray(list) || list.length === 0) {
        wrap.innerHTML = `<p class="muted">받은 요청이 없습니다.</p>`;
        return;
      }

      // FriendDto 필드명도 확정이 아니라 안전하게 처리
      wrap.innerHTML = `
        <div class="card-container">
          ${list.map(f => {
			const friendshipId = f.id;
			const sender = f.senderName;

            return `
              <div class="card open" style="cursor:default;">
                <div class="ctitle">${sender}</div>
                <div class="card-actions">
                  <button class="btn-accept" data-id="${friendshipId}">수락</button>
                  <button class="btn-reject" data-id="${friendshipId}">거절</button>
                </div>
              </div>
            `;
          }).join("")}
        </div>
      `;

      // 수락/거절 이벤트 위임
      wrap.addEventListener("click", async (e) => {
        const acceptBtn = e.target.closest(".btn-accept");
        const rejectBtn = e.target.closest(".btn-reject");

        if (!acceptBtn && !rejectBtn) return;

        const id = (acceptBtn || rejectBtn).dataset.id;
        if (!id) return alert("friendshipId가 없습니다. FriendDto 필드를 확인해주세요.");

        try {
          if (acceptBtn) {
            await fetchJson(`/friends/${id}/accept`, { method: "POST" });
            alert("친구 요청을 수락했습니다.");
          } else {
            await fetchJson(`/friends/${id}/reject`, { method: "POST" });
            alert("친구 요청을 거절했습니다.");
          }
          loadFriendRequests(); // 갱신
        } catch (err) {
          console.error(err);
          alert("처리에 실패했습니다.");
        }
      });

    } catch (e) {
      console.error(e);
      $("#requestList").innerHTML = `<p class="muted">요청 목록을 불러오지 못했습니다.</p>`;
    }
  }
  async function loadFriendList() {
    state.view = "friendsList";
    renderFriendsLayout();
    const view = $("#friendsView");
    if (!view) return;
    if (CURRENT_USER_ID === null) {
      view.innerHTML = `<p>로그인 후 이용 가능합니다.</p>`;
      return;
    }
    view.innerHTML = `
      <h3>내 친구 목록</h3>
      <div id="friendList"><p>불러오는 중...</p></div>
    `;
    try {
      // ✅ GET /friends/list
      const res = await fetchJson(`/friends/list`);
      const list = res?.data ?? [];

      const wrap = $("#friendList");
      if (!wrap) return;

      if (!Array.isArray(list) || list.length === 0) {
        wrap.innerHTML = `<p class="muted">친구가 없습니다.</p>`;
        return;
      }
      wrap.innerHTML = `
        <div class="card-container">
          ${list.map(f => {
			const friendshipId = f.id;
			const name =
			  Number(f.senderId) === Number(CURRENT_USER_ID)
			    ? escapeHtml(f.receiverName)
			    : escapeHtml(f.senderName);


            return `
              <div class="card open" style="cursor:default;">
                <div class="ctitle">${name}</div>
                <div class="card-actions">
                  <button class="btn-delete-friend" data-id="${friendshipId}">삭제</button>
                </div>
              </div>
            `;
          }).join("")}
        </div>
      `;
      wrap.addEventListener("click", async (e) => {
        const btn = e.target.closest(".btn-delete-friend");
        if (!btn) return;

        const id = btn.dataset.id;
        if (!id) return alert("friendshipId가 없습니다. FriendDto 필드를 확인해주세요.");

        if (!confirm("친구를 삭제하시겠습니까?")) return;

        try {
          // ✅ DELETE /friends/{friendshipId}
          await fetchJson(`/friends/${id}`, { method: "DELETE" });
          alert("친구가 삭제되었습니다.");
          loadFriendList();
        } catch (err) {
          console.error(err);
          alert("친구 삭제에 실패했습니다.");
        }
      });

    } catch (e) {
      console.error(e);
      $("#friendList").innerHTML = `<p class="muted">친구 목록을 불러오지 못했습니다.</p>`;
    }
  }
  
  
  // -------------------------
  // Render Recruit Cards (공통)
  // -------------------------
  function renderRecruitCards(list, opts = { mode: "HOME" }) {
    const mode = opts.mode;

    list.sort((a, b) => Number(b.id) - Number(a.id));

    return list.map((item) => {
      const isClosed = item.status === "CLOSED" || item.status === "COMPLETED";

      const isOwner =
        typeof CURRENT_USER_ID !== "undefined" &&
        CURRENT_USER_ID !== null &&
        Number(CURRENT_USER_ID) === Number(item.creatorId);

      let ownerBtns = "";
      if (mode === "MYPAGE_MYPOSTS") {
        // ✅ 내가 쓴 글 화면: 마감 버튼(마감된 글이면 숨김)
        ownerBtns = (!isClosed)
          ? `<div class="card-actions">
               <button class="btn-complete" data-id="${item.id}">마감</button>
             </div>`
          : "";
      }

      // ✅ 신청한 글 화면: 버튼 없음
      if (mode === "MYPAGE_APPLIED") {
        ownerBtns = "";
      }

      const onOff = isClosed ? "closed" : "open";

      return `
        <div class="card ${onOff}" data-status="${item.status}" data-id="${item.id}">
          <div class="ctitle">${escapeHtml(item.title ?? "")}</div>
          <div class="authorSpotsTime">
            <div class="meta">작성자: ${escapeHtml(item.creatorName ?? "")}</div>
            <div class="meta">인원: ${item.currentSpots} / ${item.totalSpots}</div>
            <div class="meta">
              마감까지:
              <span class="countdown" data-end-time="${item.endTime ?? ""}"></span>
            </div>
          </div>
          ${ownerBtns}
        </div>
      `;
    }).join("");
  }



  function escapeHtml(str) {
    return String(str ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }


  // -------------------------
  // COMPLETE (공통)
  // -------------------------
  async function completeRecruit(recruitId) {
    await fetchJson(`/recruitments/${recruitId}/complete`, { method: "POST" });
  }

  function refreshCurrentList() {
    if (state.view === "homeTeam") {
      loadHomeTeam(state.home.page);
    } else if (state.view === "myPosts") {
      loadMyPosts(state.my.page);
    } else if (state.view === "myApplied") {
      loadMyApplied(state.applied.page);
    } else if (state.view === "friendsFind") {
      loadFriendFind();
    } else if (state.view === "friendsRequests") {
      loadFriendRequests();
    } else if (state.view === "friendsList") {
      loadFriendList();
    }
  }



  function initMyPage() {
    const teamTab = $("#teamTab");
    const friendsTab = $("#friendsTab");
    const subWrap = $("#teamSubTabs");
    const myPostsTab = $("#myPostsTab");
    const myAppliedTab = $("#myAppliedTab");
	const friendsSubTabs = $("#friendsSubTabs");
	const friendFindTab = $("#friendFindTab");
	const friendRequestsTab = $("#friendRequestsTab");
	const friendListTab = $("#friendListTab");

	teamTab?.addEventListener("click", () => {
	  setActiveTab(teamTab, friendsTab, teamTab);

	  if (subWrap) subWrap.style.display = "flex";
	  if (friendsSubTabs) friendsSubTabs.style.display = "none"; // ✅ 추가

	  setActiveTab(myPostsTab, myAppliedTab, myPostsTab);
	  loadMyPosts(0);
	});


	friendsTab?.addEventListener("click", () => {
	  setActiveTab(teamTab, friendsTab, friendsTab);

	  if (subWrap) subWrap.style.display = "none";
	  if (friendsSubTabs) friendsSubTabs.style.display = "flex";

	  // 기본: 친구찾기
	  [friendFindTab, friendRequestsTab, friendListTab].forEach(el => el?.classList.remove("active-tab"));
	  friendFindTab?.classList.add("active-tab");
	  loadFriendFind();
	});


	// 하위탭 클릭
	friendFindTab?.addEventListener("click", () => {
	  setActive3(friendFindTab, friendRequestsTab, friendListTab, friendFindTab);
	  loadFriendFind();
	});

	friendRequestsTab?.addEventListener("click", () => {
	  setActive3(friendFindTab, friendRequestsTab, friendListTab, friendRequestsTab);
	  loadFriendRequests();
	});
	
	friendListTab?.addEventListener("click", () => {
	  setActive3(friendFindTab, friendRequestsTab, friendListTab, friendListTab);
	  loadFriendList();
	});

    myPostsTab?.addEventListener("click", () => {
      setActiveTab(myPostsTab, myAppliedTab, myPostsTab);
      loadMyPosts(0);
    });

    myAppliedTab?.addEventListener("click", () => {
      setActiveTab(myPostsTab, myAppliedTab, myAppliedTab);
      loadMyApplied(0);
    });

    // ✅ 첫 진입: 팀 활성 + 내가 쓴 글 활성
    setActiveTab(teamTab, friendsTab, teamTab);
    if (subWrap) subWrap.style.display = "flex";
    setActiveTab(myPostsTab, myAppliedTab, myPostsTab);
    loadMyPosts(0);
  }
  
  
  // -------------------------
  // DETAIL MODAL (전체)
  // -------------------------
  async function openDetailModal(recruitId) {
    const idNum = Number(recruitId);
    if (!Number.isFinite(idNum)) return;

    // 1) 상세 조회
    const body = await fetchJson(`/recruitments/${idNum}`);
    const d = body?.data ?? body;

    if (!d) throw new Error("No detail data");

    // 2) 신청자 목록 (DTO 오타 그대로)
    const applications = Array.isArray(d.apllications) ? d.apllications : [];

    // 3) 공통 상태 계산
    const isClosed = d.status === "CLOSED" || d.status === "COMPLETED";
    const isOwner =
      (CURRENT_USER_ID != null) &&
      (d.creatorId != null) &&
      (Number(d.creatorId) === Number(CURRENT_USER_ID));

    const isFull =
      Number(d.currentSpots ?? 0) >= Number(d.totalSpots ?? 0);

    const applied = hasApplied(applications); // applications 내 userId == CURRENT_USER_ID

    // 4) 모달 내용 채우기
    if (detailTitle) detailTitle.textContent = d.title ?? "-";
    if (detailCreator) detailCreator.textContent = d.creatorName ?? "-";
    if (detailSpots) detailSpots.textContent = `${d.currentSpots ?? 0} / ${d.totalSpots ?? 0}`;
    if (detailStatus) detailStatus.textContent = d.status ?? "-";
    if (detailDesc) detailDesc.textContent = d.description ?? "-";

    if (detailCountdown) detailCountdown.setAttribute("data-end-time", d.endTime ?? "");
    if (detailModal) detailModal.setAttribute("data-status", d.status ?? "");

    // 신청자 목록 렌더링 (userId/name/order 기준)
    const appsWrap = $("#detailApps");
    if (appsWrap) {
      appsWrap.innerHTML = applications.length
        ? `<ul class="apps-list">
            ${applications.map(a => {
              const nm = escapeHtml(a?.name ?? "알 수 없음");
              const ord = (a?.order != null) ? ` (#${a.order})` : "";
              return `<li>${nm}${ord}</li>`;
            }).join("")}
          </ul>`
        : `<p class="muted">신청자가 없습니다.</p>`;
    }

    // 5) 버튼 기본 초기화 (일단 다 숨김)
    if (detailApplyBtn) {
      detailApplyBtn.style.display = "none";
      detailApplyBtn.disabled = false;
      detailApplyBtn.textContent = "신청하기";
      detailApplyBtn.dataset.id = String(d.id ?? idNum);
      detailApplyBtn.classList.add("btn-apply");
    }

    if (detailCompleteBtn) {
      detailCompleteBtn.style.display = "none";
      detailCompleteBtn.dataset.id = String(d.id ?? idNum);
    }

    // 6) 화면별 버튼 정책 적용
    // - myPosts: 마감 버튼만(마감된 글이면 숨김)
    // - myApplied: 버튼 둘 다 없음
    // - homeTeam: (원하면) 신청/마감 정책 적용 가능
    if (state.view === "myPosts") {
      // 내가 쓴 글: 마감 버튼만
      if (detailCompleteBtn) {
        detailCompleteBtn.style.display = (!isClosed) ? "inline-block" : "none";
      }
      // 신청 버튼은 항상 숨김 유지
    } else if (state.view === "myApplied") {
      // 내가 신청한 글: 버튼 없음(상세만)
      // 둘 다 숨김 유지
    } else {
      // 홈(또는 기타): 여기서는 "상세에서 신청" UX를 그대로 유지한다고 가정
      // - 작성자면 마감 버튼
      // - 작성자 아니면 신청 버튼
      // - 마감/정원초과/이미신청이면 신청 비활성
      if (isOwner) {
        if (detailCompleteBtn) {
          detailCompleteBtn.style.display = (!isClosed) ? "inline-block" : "none";
        }
      } else {
        if (detailApplyBtn) {
          if (isClosed) {
            detailApplyBtn.style.display = "none";
          } else {
            detailApplyBtn.style.display = "inline-block";
            // 이미 신청했거나 정원 초과면 비활성화
            if (applied || isFull) {
              detailApplyBtn.disabled = true;
              detailApplyBtn.textContent = applied ? "신청완료" : "정원마감";
            } else {
              detailApplyBtn.disabled = false;
              detailApplyBtn.textContent = "신청하기";
            }
          }
        }
      }
    }

    // 7) 모달 오픈 + 카운트다운 시작
    if (detailModal) detailModal.classList.remove("hidden");
    startCountdown();
  }





  function closeDetailModal() {
    detailModal?.classList.add("hidden");
    if (detailCountdown) detailCountdown.setAttribute("data-end-time", "");
    if (detailCompleteBtn) detailCompleteBtn.dataset.id = "";
  }

  // 상세 모달 닫기 이벤트
  $("#detailCloseBtn")?.addEventListener("click", () => {
    detailModal?.classList.add("hidden");
  });
  detailModal?.addEventListener("click", (e) => {
    if (e.target === detailModal) detailModal.classList.add("hidden");
  });

  detailApplyBtn?.addEventListener("click", async () => {
    if (detailApplyBtn.disabled) return;

    const id = detailApplyBtn.dataset.id;
    if (!id) return;

    if (!confirm("이 모집에 신청하시겠습니까?")) return;

    try {
      await applyRecruit(id);

      alert("신청이 완료되었습니다!");

      // 버튼 즉시 비활성화
      detailApplyBtn.textContent = "신청완료";
      detailApplyBtn.disabled = true;

      // 신청자 목록 갱신
      refreshCurrentList();
    } catch (e) {
      console.error(e);
      alert("신청에 실패했습니다.");
    }
  });


  // 상세 모달에서 마감
  detailCompleteBtn?.addEventListener("click", async () => {
    const id = detailCompleteBtn.dataset.id;
    if (!id) return;

    if (!confirm("이 모집을 마감하시겠습니까?")) return;
    try {
      await completeRecruit(id);
      closeDetailModal();
      refreshCurrentList();
    } catch (e) {
      console.error(e);
      alert("마감 처리 중 오류가 발생했습니다.");
    }
  });

  // -------------------------
  // HOME: 팀(모집글 목록) + 게시판
  // -------------------------
  async function loadHomeTeam(page = state.home.page) {
    state.view = "homeTeam";
    setLoading();

    try {
      const body = await fetchJson(`/recruitments/lists?page=${page}&size=${state.home.size}`);

      if (!body?.success || !body.data || !Array.isArray(body.data.content)) {
        setError("응답 형식이 올바르지 않습니다.");
        return;
      }

      state.home.page = body.data.number ?? page;

      const list = body.data.content;
      if (list.length === 0) {
        setError("현재 모집 중인 글이 없습니다.");
        return;
      }

      contentArea.innerHTML = `
        <div class="list-header">
          <h2>밥친구 모집글</h2>
          <button id="openRecruitFormBtn" class="recruit-btn">모집하기</button>
        </div>

        <div class="card-container">
          ${renderRecruitCards(list)}
        </div>

        ${renderPagination(body.data)}
      `;

      // 모집하기 모달 열기
	  $("#openRecruitFormBtn")?.addEventListener("click", () => {
	    recruitModal?.classList.remove("hidden");
	    resetFriendSelectionUI(); // ✅
	  });

      // pagination
      $("#prevPageBtn")?.addEventListener("click", () => {
        if (!body.data.first) loadHomeTeam(state.home.page - 1);
      });
      $("#nextPageBtn")?.addEventListener("click", () => {
        if (!body.data.last) loadHomeTeam(state.home.page + 1);
      });

      startCountdown();
    } catch (e) {
      console.error(e);
      setError();
    }
  }

  function showHomeBoard() {
    state.view = "homeTeam"; // 게시판은 아직 별도 페이지 상태가 없으니 homeTeam 유지
    contentArea.innerHTML = `
      <h2>게시판</h2>
      <p>여기에 게시판 내용을 추가할 예정입니다.</p>
    `;
  }

  function initHome() {
    homeTabs.team.classList.add("tab-btn");
    homeTabs.board.classList.add("tab-btn");

    homeTabs.team.addEventListener("click", () => {
      setActiveTab(homeTabs.team, homeTabs.board, homeTabs.team);
      loadHomeTeam(0);
    });

    homeTabs.board.addEventListener("click", () => {
      setActiveTab(homeTabs.team, homeTabs.board, homeTabs.board);
      showHomeBoard();
    });

    // 첫 진입: 팀 자동 로드 + 탭 강조
    setActiveTab(homeTabs.team, homeTabs.board, homeTabs.team);
    loadHomeTeam(0);

    // 모집 모달 닫기
	closeRecruitBtn?.addEventListener("click", () => {
	  recruitModal?.classList.add("hidden");
	  recruitForm?.reset();
	  resetFriendSelectionUI(); // ✅
	});


	recruitModal?.addEventListener("click", (e) => {
	  if (e.target === recruitModal) {
	    recruitModal.classList.add("hidden");
	    recruitForm?.reset();
	    resetFriendSelectionUI(); // ✅ 추가
	  }
	});


    // 모집하기 submit (endHours/endMinutes -> endTime(LocalDateTime))
    recruitForm?.addEventListener("submit", async (e) => {
      e.preventDefault();

      if (CURRENT_USER_ID === null) {
        alert("로그인 후 이용해주세요.");
        return;
      }

      const formData = new FormData(recruitForm);

      const title = (formData.get("title") || "").toString().trim();
      const description = (formData.get("description") || "").toString().trim();
      const totalSpots = Number(formData.get("totalSpots"));

      const endHours = Number(formData.get("endHours") || 0);
      const endMinutes = Number(formData.get("endMinutes") || 0);

      if (!title) return alert("제목을 입력해주세요.");
      if (!description) return alert("내용을 입력해주세요.");
      if (!Number.isFinite(totalSpots) || totalSpots < 1) return alert("모집 인원을 확인해주세요.");

      const totalMins = endHours * 60 + endMinutes;
      if (!Number.isFinite(totalMins) || totalMins <= 0) {
        return alert("마감 시간을 0분보다 크게 입력해주세요.");
      }

      const end = new Date(Date.now() + totalMins * 60 * 1000);
      const pad = (n) => String(n).padStart(2, "0");
      const endTime =
        `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}` +
        `T${pad(end.getHours())}:${pad(end.getMinutes())}:${pad(end.getSeconds())}`;

		const invitedFriendIds = Array.from(state.selectedFriendIds); // ✅ 추가
		const payload = { title, description, totalSpots, endTime, invitedFriendIds }; // ✅

      try {
        await fetchJson(`/recruitments/posts`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });

        recruitModal?.classList.add("hidden");
        recruitForm?.reset();
        loadHomeTeam(0);
      } catch (err) {
        console.error(err);
        alert("모집글 작성 중 오류가 발생했습니다.");
      }
    });
  }
  
  async function loadFriendPicker() {
    const picker = $("#friendPicker");
    const listWrap = $("#friendPickerList");
    if (!picker || !listWrap) return;

    picker.classList.remove("hidden");
    listWrap.innerHTML = `<p>불러오는 중...</p>`;

    try {
      const res = await fetchJson(`/friends/list`);
      const list = res?.data ?? [];

      if (!Array.isArray(list) || list.length === 0) {
        listWrap.innerHTML = `<p class="muted">친구가 없습니다.</p>`;
        return;
      }

      listWrap.innerHTML = `
        <div class="card-container">
          ${list.map(f => {
            const id = f.id; // ⚠️ 여기 f.id가 "friendshipId"일 수도 있음 (아래 백엔드 설계 참고)
            const name =
              Number(f.senderId) === Number(CURRENT_USER_ID)
                ? escapeHtml(f.receiverName)
                : escapeHtml(f.senderName);

            const friendUserId =
              Number(f.senderId) === Number(CURRENT_USER_ID) ? f.receiverId : f.senderId; // ✅ userId 추출

            const disabled = state.selectedFriendIds.has(Number(friendUserId));
            return `
              <div class="card open" style="cursor:default;">
                <div class="ctitle">${name}</div>
                <div class="card-actions">
                  <button class="btn-pick-friend" data-user-id="${friendUserId}" data-name="${name}" ${disabled ? "disabled" : ""}>
                    ${disabled ? "선택됨" : "넣기"}
                  </button>
                </div>
              </div>
            `;
          }).join("")}
        </div>
      `;

    } catch (e) {
      console.error(e);
      listWrap.innerHTML = `<p class="muted">친구 목록을 불러오지 못했습니다.</p>`;
    }
  }

  function renderSelectedFriends() {
    const selected = $("#selectedFriends");
    if (!selected) return;

    const ids = Array.from(state.selectedFriendIds);
    if (ids.length === 0) {
      selected.innerHTML = `<p class="muted">선택된 친구 없음</p>`;
      return;
    }

    selected.innerHTML = `
      <div style="display:flex; gap:6px; flex-wrap:wrap;">
        ${ids.map(id => {
          const name = state.selectedFriendNames.get(Number(id)) || `ID:${id}`;
          return `
            <span style="border:1px solid #ddd; padding:4px 8px; border-radius:999px;">
              ${escapeHtml(name)}
              <button type="button" class="btn-remove-picked" data-user-id="${id}" style="margin-left:6px;">x</button>
            </span>
          `;
        }).join("")}
      </div>
    `;
  }


  // -------------------------
  // MYPAGE: 내가쓴글 + 친구
  // -------------------------
  async function loadMyPosts(page = state.my.page) {
    state.view = "myPosts";
    if (CURRENT_USER_ID === null) {
      contentArea.innerHTML = `<h2>내가 쓴 글</h2><p>로그인 후 이용 가능합니다.</p>`;
      return;
    }

    setLoading();
    const body = await fetchJson(`/recruitments/my/posts?page=${page}&size=${state.my.size}`);

    if (!body?.success || !body.data || !Array.isArray(body.data.content)) {
      setError("응답 형식이 올바르지 않습니다.");
      return;
    }

    state.my.page = body.data.number ?? page;

    const list = body.data.content;
    if (list.length === 0) {
      contentArea.innerHTML = `<h2>내가 쓴 글</h2><p>아직 작성한 글이 없습니다.</p>`;
      return;
    }

    contentArea.innerHTML = `
      <h2>내가 쓴 글</h2>
      <div class="card-container">
        ${renderRecruitCards(list, { mode: "MYPAGE_MYPOSTS" })}
      </div>
      ${renderPagination(body.data)}
    `;

    $("#prevPageBtn")?.addEventListener("click", () => { if (!body.data.first) loadMyPosts(state.my.page - 1); });
    $("#nextPageBtn")?.addEventListener("click", () => { if (!body.data.last) loadMyPosts(state.my.page + 1); });

    startCountdown();
  }

  function resetFriendSelectionUI() {
    // ✅ 재할당 X, 기존 객체 비우기
    state.selectedFriendIds.clear();
    state.selectedFriendNames.clear();

    const selected = $("#selectedFriends");
    if (selected) selected.innerHTML = `<p class="muted">선택된 친구 없음</p>`;

    const picker = $("#friendPicker");
    if (picker) picker.classList.add("hidden");
  }


  
  async function loadMyApplied(page = state.applied.page) {
    state.view = "myApplied";
    if (CURRENT_USER_ID === null) {
      contentArea.innerHTML = `<h2>내가 신청한 글</h2><p>로그인 후 이용 가능합니다.</p>`;
      return;
    }

    setLoading();
    // ✅ 네 컨트롤러 매핑 그대로 사용 (대소문자 주의!)
    const body = await fetchJson(`/recruitments/apply/my/ApplyList?page=${page}&size=${state.applied.size}`);

    // 응답 타입이 Page로 오면 아래 그대로, 아니면 body.data 형태에 맞게 조정해야 함
    if (!body?.success || !body.data || !Array.isArray(body.data.content)) {
      setError("응답 형식이 올바르지 않습니다.");
      return;
    }

    state.applied.page = body.data.number ?? page;

    const list = body.data.content;
    if (list.length === 0) {
      contentArea.innerHTML = `<h2>내가 신청한 글</h2><p>아직 신청한 글이 없습니다.</p>`;
      return;
    }

    contentArea.innerHTML = `
      <h2>내가 신청한 글</h2>
      <div class="card-container">
        ${renderRecruitCards(list, { mode: "MYPAGE_APPLIED" })}
      </div>
      ${renderPagination(body.data)}
    `;

    $("#prevPageBtn")?.addEventListener("click", () => { if (!body.data.first) loadMyApplied(state.applied.page - 1); });
    $("#nextPageBtn")?.addEventListener("click", () => { if (!body.data.last) loadMyApplied(state.applied.page + 1); });

    startCountdown();
  }

  function setActive3(a, b, c, active) {
    [a, b, c].forEach(el => {
      if (!el) return;
      el.classList.add("tab-btn");
      el.classList.toggle("active-tab", el === active);
    });
  }


  function loadFriends() {
    contentArea.innerHTML = `<h2>친구</h2><p>여기에 친구 기능 UI를 넣을 예정입니다.</p>`;
  }

  // -------------------------
  // GLOBAL EVENT DELEGATION
  // - dynamic render 대응
  // -------------------------
  // 카드 클릭 -> 상세 열기
  contentArea?.addEventListener("click", (e) => {
    const target = e.target;

    // 마감 버튼 클릭이면 카드 클릭으로 안 넘어가게
    if (target.classList.contains("btn-complete")) return;

    const card = target.closest(".card");
    if (card?.dataset?.id) {
      openDetailModal(card.dataset.id).catch((err) => {
        console.error(err);
        alert("상세 정보를 불러오지 못했습니다.");
      });
    }
  });

  // 리스트 카드의 "마감" 버튼 (이벤트 위임)
  contentArea?.addEventListener("click", async (e) => {
    const btn = e.target.closest(".btn-complete");
    if (!btn) return;

    e.stopPropagation();

    const id = btn.dataset.id;
    if (!id) return;

    if (!confirm("이 모집을 마감하시겠습니까?")) return;

    try {
      await completeRecruit(id);
      refreshCurrentList();
    } catch (err) {
      console.error(err);
      alert("마감 처리 중 오류가 발생했습니다.");
    }
  });

  // -------------------------
  // Init by page
  // -------------------------
  if (isHome) initHome();
  if (isMyPage) initMyPage();
});
