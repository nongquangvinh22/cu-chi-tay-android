(function () {
  const local = window.NQVAuth;
  const cfg = window.NQV_SUPABASE_CONFIG || {};
  const ready = !!(window.supabase && cfg.url && cfg.anonKey && !cfg.url.includes("YOUR_") && !cfg.anonKey.includes("YOUR_"));
  const client = ready ? window.supabase.createClient(cfg.url, cfg.anonKey, { auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true } }) : null;
  let profile = null;
  let saveChain = Promise.resolve();
  const defaults = () => ({ xp: 0, completedLessons: [], passedExams: [], examAttempts: {}, appointment: "", daysStudied: [], world: { x: 8, y: 58 }, lastUpdated: new Date().toISOString() });
  function cache(p) {
    try { const all = JSON.parse(localStorage.getItem("nqv_hsk_cloud_cache") || "{}"); all[p.id] = p; localStorage.setItem("nqv_hsk_cloud_cache", JSON.stringify(all)); } catch (e) { console.warn(e); }
  }
  async function load(user) {
    const { data, error } = await client.from("player_profiles").select("display_name, username, progress").eq("id", user.id).maybeSingle();
    if (error) throw error;
    if (!data) {
      const p = { id: user.id, name: user.user_metadata?.display_name || user.email.split("@")[0], username: user.email, email: user.email, kind: "cloud", progress: defaults() };
      const { error: insertError } = await client.from("player_profiles").insert({ id: p.id, display_name: p.name, username: p.username, progress: p.progress });
      if (insertError) throw insertError;
      return p;
    }
    return { id: user.id, name: data.display_name || "Sĩ tử", username: data.username || user.email, email: user.email, kind: "cloud", progress: data.progress || defaults() };
  }
  function notify() {
    window.dispatchEvent(new CustomEvent("nqv-profile-changed", { detail: profile }));
    window.dispatchEvent(new CustomEvent("nqv-progress-changed", { detail: profile?.progress }));
  }
  function sync() {
    if (!ready || !profile || profile.kind !== "cloud") return;
    const snapshot = JSON.parse(JSON.stringify(profile.progress));
    saveChain = saveChain.then(() => client.from("player_profiles").upsert({
      id: profile.id, display_name: profile.name, username: profile.username, progress: snapshot, updated_at: new Date().toISOString()
    }, { onConflict: "id" })).then(({ error }) => {
      if (error) throw error;
      window.dispatchEvent(new CustomEvent("nqv-sync-status", { detail: { ok: true } }));
    }).catch(error => {
      console.error("NQV cloud sync:", error);
      window.dispatchEvent(new CustomEvent("nqv-sync-status", { detail: { ok: false, message: "Chưa đồng bộ được; tiến trình vẫn còn trên thiết bị." } }));
    });
  }
  window.NQVAuth = {
    cloudReady: () => ready,
    current: () => profile,
    async init() {
      if (ready) {
        const { data: { session }, error } = await client.auth.getSession();
        if (error) throw error;
        if (session?.user) { profile = await load(session.user); cache(profile); return profile; }
      }
      profile = await local.init();
      return profile;
    },
    async register(email, password, displayName, remember) {
      if (!ready) throw new Error("Chưa kết nối máy chủ tài khoản. Hãy hoàn tất cấu hình Supabase trước khi đăng ký trực tuyến.");
      const clean = email.trim().toLowerCase();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(clean)) throw new Error("Vui lòng nhập email hợp lệ.");
      if (password.length < 8) throw new Error("Mật khẩu cần ít nhất 8 ký tự.");
      const guestProgress = profile?.kind === "guest" ? profile.progress : defaults();
      const { data, error } = await client.auth.signUp({ email: clean, password, options: { data: { display_name: displayName.trim() || clean.split("@")[0] } } });
      if (error) throw error;
      if (!data.session || !data.user) throw new Error("Đã gửi email xác nhận. Hãy xác nhận email rồi đăng nhập.");
      profile = { id: data.user.id, name: displayName.trim() || clean.split("@")[0], username: clean, email: clean, kind: "cloud", progress: guestProgress };
      const { error: saveError } = await client.from("player_profiles").upsert({ id: profile.id, display_name: profile.name, username: clean, progress: guestProgress });
      if (saveError) throw saveError;
      cache(profile); notify(); return profile;
    },
    async login(email, password, remember) {
      if (!ready) throw new Error("Chưa kết nối máy chủ tài khoản. Hãy hoàn tất cấu hình Supabase trước khi đăng nhập.");
      const { data, error } = await client.auth.signInWithPassword({ email: email.trim().toLowerCase(), password });
      if (error) throw error;
      profile = await load(data.user); cache(profile); notify(); return profile;
    },
    continueAsGuest() { profile = local.continueAsGuest(); return profile; },
    save() { if (profile?.kind === "cloud") { cache(profile); sync(); } else local.save(); },
    updateProgress(mutator) {
      if (!profile) return null;
      mutator(profile.progress);
      profile.progress.lastUpdated = new Date().toISOString();
      if (profile.kind === "cloud") { cache(profile); sync(); }
      else local.updateProgress(p => Object.assign(p, profile.progress));
      window.dispatchEvent(new CustomEvent("nqv-progress-changed", { detail: profile.progress }));
      return profile.progress;
    },
    async logout() {
      if (profile?.kind === "cloud" && ready) { const { error } = await client.auth.signOut(); if (error) throw error; }
      else await local.logout();
      profile = null;
      window.dispatchEvent(new CustomEvent("nqv-profile-changed", { detail: null }));
    }
  };
})();