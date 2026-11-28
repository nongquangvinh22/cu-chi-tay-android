/* Dữ liệu học liệu ban đầu. Có thể mở rộng dần bộ từ vựng và dạng bài lên chuẩn HSK đầy đủ. */
window.NQV_DATA = {
  ranks: ["Đồng sinh", "Tú tài", "Cử nhân", "Tiến sĩ", "Hoàng giáp", "Trạng nguyên"],
  stages: [
    { id: 1, hsk: "HSK 1", title: "Khai môn nhập học", rank: "Đồng sinh", glyph: "學", note: "Nhận mặt chữ, chào hỏi và giới thiệu bản thân.", exam: "Khai môn khảo", lessons: [
      { id: "h1-a", title: "Nét bút đầu tiên", subtitle: "Chào hỏi · Đại từ · Lời đáp", glyph: "人", words: [
        { hanzi: "你好", pinyin: "nǐ hǎo", meaning: "Xin chào", example: "你好！很高兴认识你。", translation: "Xin chào! Rất vui được làm quen với bạn." },
        { hanzi: "谢谢", pinyin: "xiè xie", meaning: "Cảm ơn", example: "谢谢老师。", translation: "Cảm ơn thầy/cô." },
        { hanzi: "再见", pinyin: "zài jiàn", meaning: "Tạm biệt", example: "明天见，再见！", translation: "Hẹn gặp ngày mai, tạm biệt!" },
        { hanzi: "我", pinyin: "wǒ", meaning: "Tôi, mình", example: "我是学生。", translation: "Tôi là học sinh." }
      ] },
      { id: "h1-b", title: "Làm quen thế gian", subtitle: "Gia đình · Con số · Trường lớp", glyph: "家", words: [
        { hanzi: "老师", pinyin: "lǎo shī", meaning: "Giáo viên", example: "她是我的老师。", translation: "Cô ấy là giáo viên của tôi." },
        { hanzi: "学生", pinyin: "xué sheng", meaning: "Học sinh", example: "我是中国学生。", translation: "Tôi là học sinh Trung Quốc." },
        { hanzi: "朋友", pinyin: "péng you", meaning: "Bạn bè", example: "他是我的朋友。", translation: "Anh ấy là bạn của tôi." },
        { hanzi: "家", pinyin: "jiā", meaning: "Nhà, gia đình", example: "我家有四个人。", translation: "Nhà tôi có bốn người." }
      ] }
    ] },
    { id: 2, hsk: "HSK 2", title: "Đồng sinh khảo học", rank: "Tú tài", glyph: "書", note: "Diễn đạt sinh hoạt, thời gian và nhu cầu thường ngày.", exam: "Đồng sinh khảo", lessons: [
      { id: "h2-a", title: "Một ngày thường nhật", subtitle: "Thời gian · Hoạt động · Ăn uống", glyph: "日", words: [
        { hanzi: "早上", pinyin: "zǎo shang", meaning: "Buổi sáng", example: "我早上七点起床。", translation: "Tôi thức dậy lúc bảy giờ sáng." },
        { hanzi: "已经", pinyin: "yǐ jīng", meaning: "Đã, rồi", example: "我已经吃饭了。", translation: "Tôi đã ăn cơm rồi." },
        { hanzi: "觉得", pinyin: "jué de", meaning: "Cảm thấy, cho rằng", example: "我觉得中文很有意思。", translation: "Tôi cảm thấy tiếng Trung rất thú vị." },
        { hanzi: "一起", pinyin: "yì qǐ", meaning: "Cùng nhau", example: "我们一起学习。", translation: "Chúng ta cùng nhau học tập." }
      ] },
      { id: "h2-b", title: "Đi xa mở mang", subtitle: "Địa điểm · Phương hướng · Giao thông", glyph: "行", words: [
        { hanzi: "附近", pinyin: "fù jìn", meaning: "Gần đây, lân cận", example: "学校附近有商店。", translation: "Gần trường học có cửa hàng." },
        { hanzi: "帮助", pinyin: "bāng zhù", meaning: "Giúp đỡ", example: "谢谢你帮助我。", translation: "Cảm ơn bạn đã giúp tôi." },
        { hanzi: "准备", pinyin: "zhǔn bèi", meaning: "Chuẩn bị", example: "我准备去北京。", translation: "Tôi chuẩn bị đi Bắc Kinh." },
        { hanzi: "因为", pinyin: "yīn wèi", meaning: "Bởi vì", example: "因为下雨，所以我不去。", translation: "Vì trời mưa nên tôi không đi." }
      ] }
    ] },
    { id: 3, hsk: "HSK 3", title: "Tú tài nhập trường", rank: "Cử nhân", glyph: "文", note: "Mở rộng vốn từ, kể chuyện và trình bày ý kiến.", exam: "Tú tài khảo", lessons: [
      { id: "h3-a", title: "Tâm tình và chí hướng", subtitle: "Cảm xúc · Suy nghĩ · Mục tiêu", glyph: "心", words: [
        { hanzi: "影响", pinyin: "yǐng xiǎng", meaning: "Ảnh hưởng", example: "习惯会影响生活。", translation: "Thói quen sẽ ảnh hưởng đến cuộc sống." },
        { hanzi: "决定", pinyin: "jué dìng", meaning: "Quyết định", example: "我决定每天练习。", translation: "Tôi quyết định luyện tập mỗi ngày." },
        { hanzi: "特别", pinyin: "tè bié", meaning: "Đặc biệt", example: "今天特别冷。", translation: "Hôm nay đặc biệt lạnh." },
        { hanzi: "认真", pinyin: "rèn zhēn", meaning: "Nghiêm túc, chăm chú", example: "他认真地学习。", translation: "Anh ấy học tập rất chăm chú." }
      ] },
      { id: "h3-b", title: "Chuyện kể đường xa", subtitle: "Trải nghiệm · Kể chuyện · So sánh", glyph: "路", words: [
        { hanzi: "终于", pinyin: "zhōng yú", meaning: "Cuối cùng, rốt cuộc", example: "我终于完成了作业。", translation: "Cuối cùng tôi cũng hoàn thành bài tập." },
        { hanzi: "过去", pinyin: "guò qù", meaning: "Quá khứ; đã qua", example: "过去的事情不要担心。", translation: "Đừng lo lắng về chuyện đã qua." },
        { hanzi: "比较", pinyin: "bǐ jiào", meaning: "Khá; so sánh", example: "这本书比较有趣。", translation: "Cuốn sách này khá thú vị." },
        { hanzi: "发现", pinyin: "fā xiàn", meaning: "Phát hiện, nhận ra", example: "我发现一个问题。", translation: "Tôi phát hiện một vấn đề." }
      ] }
    ] },
    { id: 4, hsk: "HSK 4", title: "Cử nhân dự khoa", rank: "Tiến sĩ", glyph: "策", note: "Hiểu sắc thái từ ngữ, lập luận và xử lý tình huống.", exam: "Hương thí", lessons: [
      { id: "h4-a", title: "Lập luận và biện giải", subtitle: "Quan điểm · Nguyên nhân · Kết quả", glyph: "論", words: [
        { hanzi: "因此", pinyin: "yīn cǐ", meaning: "Vì vậy, do đó", example: "天气不好，因此比赛取消了。", translation: "Thời tiết không tốt, vì vậy cuộc thi đã bị hủy." },
        { hanzi: "无论", pinyin: "wú lùn", meaning: "Bất luận, dù cho", example: "无论多难，我都要试试。", translation: "Dù khó đến đâu, tôi cũng muốn thử." },
        { hanzi: "提前", pinyin: "tí qián", meaning: "Sớm hơn, trước thời hạn", example: "请提前十分钟到。", translation: "Vui lòng đến sớm trước mười phút." },
        { hanzi: "适合", pinyin: "shì hé", meaning: "Phù hợp, thích hợp", example: "这份工作很适合她。", translation: "Công việc này rất phù hợp với cô ấy." }
      ] },
      { id: "h4-b", title: "Xã hội và nhân tình", subtitle: "Ứng xử · Trách nhiệm · Hợp tác", glyph: "義", words: [
        { hanzi: "责任", pinyin: "zé rèn", meaning: "Trách nhiệm", example: "每个人都有自己的责任。", translation: "Mỗi người đều có trách nhiệm của mình." },
        { hanzi: "尊重", pinyin: "zūn zhòng", meaning: "Tôn trọng", example: "我们应该尊重别人。", translation: "Chúng ta nên tôn trọng người khác." },
        { hanzi: "交流", pinyin: "jiāo liú", meaning: "Trao đổi, giao lưu", example: "多交流才能互相了解。", translation: "Giao lưu nhiều mới có thể hiểu nhau." },
        { hanzi: "安排", pinyin: "ān pái", meaning: "Sắp xếp, bố trí", example: "老师安排了新的任务。", translation: "Thầy giáo đã sắp xếp nhiệm vụ mới." }
      ] }
    ] },
    { id: 5, hsk: "HSK 5", title: "Hội thí tuyển hiền", rank: "Hoàng giáp", glyph: "經", note: "Đọc hiểu văn bản dài, thành ngữ và ngôn ngữ trừu tượng.", exam: "Hội thí", lessons: [
      { id: "h5-a", title: "Văn chương và điển cố", subtitle: "Thành ngữ · Văn hóa · Tri thức", glyph: "典", words: [
        { hanzi: "逐渐", pinyin: "zhú jiàn", meaning: "Dần dần, từng bước", example: "他的中文逐渐进步了。", translation: "Tiếng Trung của anh ấy dần tiến bộ." },
        { hanzi: "现象", pinyin: "xiàn xiàng", meaning: "Hiện tượng", example: "这是一个常见的现象。", translation: "Đây là một hiện tượng thường gặp." },
        { hanzi: "传统", pinyin: "chuán tǒng", meaning: "Truyền thống", example: "我们应该了解传统文化。", translation: "Chúng ta nên tìm hiểu văn hóa truyền thống." },
        { hanzi: "概念", pinyin: "gài niàn", meaning: "Khái niệm", example: "先理解这个概念。", translation: "Trước tiên hãy hiểu khái niệm này." }
      ] },
      { id: "h5-b", title: "Luận bàn thời cuộc", subtitle: "Xã hội · Phân tích · Quan điểm", glyph: "政", words: [
        { hanzi: "措施", pinyin: "cuò shī", meaning: "Biện pháp", example: "政府采取了新的措施。", translation: "Chính phủ đã áp dụng biện pháp mới." },
        { hanzi: "趋势", pinyin: "qū shì", meaning: "Xu hướng", example: "这是未来的发展趋势。", translation: "Đây là xu hướng phát triển trong tương lai." },
        { hanzi: "强调", pinyin: "qiáng diào", meaning: "Nhấn mạnh", example: "老师强调了重点。", translation: "Thầy giáo đã nhấn mạnh trọng điểm." },
        { hanzi: "客观", pinyin: "kè guān", meaning: "Khách quan", example: "我们要客观地看待问题。", translation: "Chúng ta cần nhìn nhận vấn đề một cách khách quan." }
      ] }
    ] },
    { id: 6, hsk: "HSK 6", title: "Điện thí trạng nguyên", rank: "Trạng nguyên", glyph: "榜", note: "Tổng hợp năng lực, suy luận sắc thái và chinh phục kỳ thi cuối.", exam: "Điện thí", lessons: [
      { id: "h6-a", title: "Học vấn uyên thâm", subtitle: "Học thuật · Sắc thái · Thành ngữ", glyph: "學", words: [
        { hanzi: "诠释", pinyin: "quán shì", meaning: "Giải thích, diễn giải", example: "这段文字可以有不同的诠释。", translation: "Đoạn văn này có thể có những cách diễn giải khác nhau." },
        { hanzi: "权衡", pinyin: "quán héng", meaning: "Cân nhắc, cân đối", example: "做决定前要权衡利弊。", translation: "Trước khi quyết định cần cân nhắc lợi và hại." },
        { hanzi: "蕴含", pinyin: "yùn hán", meaning: "Hàm chứa, ẩn chứa", example: "这句话蕴含着深刻的道理。", translation: "Câu nói này hàm chứa đạo lý sâu sắc." },
        { hanzi: "毋庸置疑", pinyin: "wú yōng zhì yí", meaning: "Không cần nghi ngờ, chắc chắn", example: "他的努力是毋庸置疑的。", translation: "Sự nỗ lực của anh ấy là điều không cần nghi ngờ." }
      ] },
      { id: "h6-b", title: "Trị thế an dân", subtitle: "Chính luận · Đạo lý · Quyết sách", glyph: "治", words: [
        { hanzi: "弊端", pinyin: "bì duān", meaning: "Tệ nạn, mặt bất cập", example: "我们必须正视制度中的弊端。", translation: "Chúng ta phải nhìn thẳng vào những bất cập trong chế độ." },
        { hanzi: "贯彻", pinyin: "guàn chè", meaning: "Quán triệt, thực hiện triệt để", example: "要贯彻这项政策。", translation: "Cần quán triệt chính sách này." },
        { hanzi: "取缔", pinyin: "qǔ dì", meaning: "Đình chỉ, cấm đoán", example: "有关部门依法取缔了非法活动。", translation: "Cơ quan liên quan đã đình chỉ hoạt động trái phép theo luật." },
        { hanzi: "卓越", pinyin: "zhuó yuè", meaning: "Xuất chúng, kiệt xuất", example: "她做出了卓越的贡献。", translation: "Cô ấy đã có những đóng góp xuất chúng." }
      ] }
    ] }
  ]
};
