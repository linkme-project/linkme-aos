package com.linkme.fido.network;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * API 호출 인터페이스
 * 없음 아직...
 */
public interface APIService {
    @GET("/v1/board/notices")
    Call<String> getNotices();
}
