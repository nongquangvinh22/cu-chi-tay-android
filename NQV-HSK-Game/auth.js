(function () {
  const ACCOUNT_KEY = "nqv_hsk_accounts_v1";
  const PROFILE_KEY = "nqv_hsk_profiles_v1";
  const COOKIE_NAME = "nqv_hsk_player";
  const PBKDF2_ROUNDS = 180000;
  let current = null;

  function readJSON(key, fallback) {
    try {
      const value = localStorage.getItem(key);
      return value ? JSON.parse(value) : fallback;
    } catch (error) {
      console.error("Không thể đọc dữ liệu hồ sơ:", error);
      return fallback;
    }
  }
  function writeJSON(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value));
      return true;
    } catch (error) {
      console.error("Không thể lưu dữ liệu hồ sơ:", error);
      throw new Error("Trình duyệt không đủ dung lượng hoặc đang chặn lưu dữ liệu.");
    }
  }
  function cookieValue(name) {
    const prefix = `${name}=`;
    const found = document.cookie.split(";").map((part) => part.trim()).find((part) => part.startsWith(prefix));
    return found ? decodeURIComponent(found.slice(prefix.length)) : null;
  }
  function setSessionCookie(id, remember) {
    const secure = location.protocol === "https:" ? "; Secure" : "";
    const age = remember ? "; Max-Age=31536000" : "";
    document.cookie = `${COOKIE_NAME}=${encodeURIComponent(id)}; Path=/; SameSite=Lax${age}${secure}`;
  }
  function clearSessionCookie() {
    document.cookie = `${COOKIE_NAME}=; Path=/; Max-Age=0; SameSite=Lax`;
  }
  function makeId(prefix) {
    const random = window.crypto && crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    return `${prefix}-${random}`;
  }
  function bytesToBase64(bytes) {
    let binary = "";
    bytes.forEach((value) => { binary += String.fromCharCode(value); });
    return btoa(binary);
  }
  function base64ToBytes(value) {
    const binary = atob(value);
    return Uint8Array.from(binary, (character) => character.charCodeAt(0));
  }
  async function derivePassword(password, salt) {
    if (!window.crypto || !crypto.subtle) {
      throw new Error("Đăng ký/đăng nhập cần trình duyệt hỗ trợ Web Crypto. Hãy mở game bằng địa chỉ HTTPS.");
    }
    const keyMaterial = await crypto.subtle.importKey("raw", new TextEncoder().encode(password), "PBKDF2", false, ["deriveBits"]);
    const bits = await crypto.subtle.deriveBits({ name: "PBKDF2", salt, iterations: PBKDF2_ROUNDS, hash: "SHA-256" }, keyMaterial, 256);
    return new Uint8Array(bits);
  }
  function defaultProgress() {
    return {
      xp: 0,
      completedLessons: [],
      passedExams: [],
      examAttempts: {},
      appointment: "",
      daysStudied: [],
      world: { x: 8, y: 58 },
      lastUpdated: new Date().toISOString()
    };
  }
  function createProfile(id, name, username, kind) {
    return {
      id,
      name: name || "Sĩ tử",
      username: username || "khach",
      kind: kind || "local",
      progress: defaultProgress(),
      createdAt: new Date().toISOString()
    };
  }
  function saveCurrent() {
    if (!current) return;
    const profiles = readJSON(PROFILE_KEY, {});
    current.progress.lastUpdated = new Date().toISOString();
    profiles[current.id] = current;
    writeJSON(PROFILE_KEY, profiles);
  }
  function activate(profile, remember) {
    current = profile;
    setSessionCookie(profile.id, remember);
    saveCurrent();
    window.dispatchEvent(new CustomEvent("nqv-profile-changed", { detail: current }));
    return current;
  }

  const api = {
    async init() {
      const id = cookieValue(COOKIE_NAME);
      const profiles = readJSON(PROFILE_KEY, {});
      if (id && profiles[id]) current = profiles[id];
      if (!current) {
        const guest = Object.values(profiles).find((profile) => profile.kind === "guest" && profile.username === "khach-mac-dinh");
        if (guest) {
          current = guest;
          setSessionCookie(guest.id, true);
        }
      }
      return current;
    },
    current() { return current; },
    async register(username, password, displayName, remember) {
      const cleanName = username.trim().toLowerCase();
      if (!/^[a-z0-9._-]{3,24}$/.test(cleanName)) throw new Error("Tên đăng nhập cần 3–24 ký tự: chữ thường, số, dấu chấm, gạch dưới hoặc gạch ngang.");
      if (password.length < 8) throw new Error("Mật khẩu cần ít nhất 8 ký tự.");
      const accounts = readJSON(ACCOUNT_KEY, {});
      if (accounts[cleanName]) throw new Error("Tên đăng nhập này đã được sử dụng trên trình duyệt này.");
      const salt = crypto.getRandomValues(new Uint8Array(16));
      const hash = await derivePassword(password, salt);
      const id = makeId("player");
      accounts[cleanName] = { id, username: cleanName, salt: bytesToBase64(salt), hash: bytesToBase64(hash), rounds: PBKDF2_ROUNDS, createdAt: new Date().toISOString() };
      writeJSON(ACCOUNT_KEY, accounts);
      const guestProgress = current && current.kind === "guest" ? current.progress : defaultProgress();
      const newProfile = createProfile(id, displayName.trim() || cleanName, cleanName, "local");
      newProfile.progress = guestProgress;
      return activate(newProfile, remember);
    },
    async login(username, password, remember) {
      const cleanName = username.trim().toLowerCase();
      const accounts = readJSON(ACCOUNT_KEY, {});
      const account = accounts[cleanName];
      if (!account) throw new Error("Không tìm thấy tài khoản trên trình duyệt này. Hãy kiểm tra tên đăng nhập hoặc đăng ký mới.");
      const hash = await derivePassword(password, base64ToBytes(account.salt));
      const expected = base64ToBytes(account.hash);
      let mismatch = hash.length ^ expected.length;
      for (let index = 0; index < Math.min(hash.length, expected.length); index += 1) mismatch |= hash[index] ^ expected[index];
      if (mismatch !== 0) throw new Error("Mật khẩu chưa chính xác.");
      const profiles = readJSON(PROFILE_KEY, {});
      const profile = profiles[account.id] || createProfile(account.id, cleanName, cleanName, "local");
      return activate(profile, remember);
    },
    continueAsGuest() {
      const profiles = readJSON(PROFILE_KEY, {});
      let guest = Object.values(profiles).find((profile) => profile.kind === "guest" && profile.username === "khach-mac-dinh");
      if (!guest) guest = createProfile(makeId("guest"), "Sĩ tử", "khach-mac-dinh", "guest");
      guest.kind = "guest";
      return activate(guest, true);
    },
    save() { saveCurrent(); },
    updateProgress(mutator) {
      if (!current) return null;
      mutator(current.progress);
      saveCurrent();
      window.dispatchEvent(new CustomEvent("nqv-progress-changed", { detail: current.progress }));
      return current.progress;
    },
    logout() {
      saveCurrent();
      current = null;
      clearSessionCookie();
      window.dispatchEvent(new CustomEvent("nqv-profile-changed", { detail: null }));
    }
  };
  window.NQVAuth = api;
})();
