package com.example.shipper.remote;

import com.example.shipper.model.FCMResponse;
import com.example.shipper.model.FCMSendData;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAA0_WCNhU:APA91bGbt7dfJ4pUHSKrAsmDoV2yYX4ptrraOlGl7FDFiMPy_tbpe-JB4SllhG03FXHmIDOZjUOr1Tk3x705PfEdio7RWuOCOneUNNM8mkCo6jcY6TAriXM3N-ONxj6vq-0-ezabT-G1"
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
