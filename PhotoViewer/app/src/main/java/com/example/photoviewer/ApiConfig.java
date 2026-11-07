package com.example.photoviewer;

public final class ApiConfig {

    private ApiConfig() {
    }

    // PythonAnywhere 클라우드 서버 (ID 12/13 검증용). 로컬 테스트로 되돌리려면
    // 아래 한 줄만 "http://10.0.2.2:8000"으로 바꾸면 된다 (다른 파일 수정 불필요).
    public static final String BASE_URL = "https://hoynola.pythonanywhere.com";
}
