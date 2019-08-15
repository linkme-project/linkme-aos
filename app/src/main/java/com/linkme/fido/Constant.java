package com.linkme.fido;

/**
 * Constant 클래스
 * 관리할게... 아직 URL밖에 없음...
 */
public class Constant {
    class Service {
        static final int BASE = 0;
        static final int TEST = 1;
    }

    public static final String URL[] = {
            "http://linkme-home.nakkulab.com/",
            "https://www.naver.com/"
    };

    public static final String BASE_URL = URL[Service.BASE];

    public static final String TEST_URL = URL[Service.TEST];
}
