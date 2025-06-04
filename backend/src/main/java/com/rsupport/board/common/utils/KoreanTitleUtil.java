package com.rsupport.board.common.utils;

import java.util.Random;

public class KoreanTitleUtil {
    private static final int HANGUL_BASE = 0xAC00; // '가'
    private static final int HANGUL_COUNT = 11172; // 가~힣 총 개수
    private static final Random rnd = new Random();

    private static final String[] ADJECTIVES = {
            "긴급", "중요", "안내", "필독", "시스템", "신규", "업데이트", "점검", "공지", "안전", "국민"
    };

    private static final String[] NOUNS = {
            "서버", "공지사항", "서비스", "계정", "장애", "패치", "점검", "이벤트", "공지", "업데이트"
    };

    private static final String[] VERB_ENDINGS = {
            "알려드립니다.", "안내합니다.", "공지드립니다.", "발표합니다.", "진행합니다.", "시작합니다.", "완료되었습니다."
    };

    /**
     * @param length 뽑고 싶은 음절 개수
     * @return length 길이의 랜덤 한글 문자열 (말안되도됨)
     */
    public static String randomHangulString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char syllable = (char) (HANGUL_BASE + rnd.nextInt(HANGUL_COUNT));
            sb.append(syllable);
        }
        return sb.toString();
    }

    /**
     * 한국어 제목 생성
     */
    public static String randomKoreanTitle() {
        String adj = ADJECTIVES[rnd.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[rnd.nextInt(NOUNS.length)];
        String verb = VERB_ENDINGS[rnd.nextInt(VERB_ENDINGS.length)];
        return adj + " " + noun + " " + verb + " " + randomHangulString(rnd.nextInt(2)+2);
    }

    public static void main(String[] args) {
    }
}

