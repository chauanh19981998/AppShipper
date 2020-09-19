package com.example.shipper.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.shipper.callback.IShippingOrderCallbackListener;
import com.example.shipper.common.Common;
import com.example.shipper.model.ShippingOrderModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel implements IShippingOrderCallbackListener {

    private MutableLiveData<List<ShippingOrderModel>> shippingOrderMutableLiveData;
    private MutableLiveData<String> messageError;

    private IShippingOrderCallbackListener listener;

    public HomeViewModel() {
        shippingOrderMutableLiveData = new MutableLiveData<>();
        messageError = new MutableLiveData<>();
        listener = this;
    }

    public MutableLiveData<List<ShippingOrderModel>> getShippingOrderMutableLiveData(String shipperPhone) {
        loadOrderByShipper(shipperPhone);
        return shippingOrderMutableLiveData;
    }

    private void loadOrderByShipper(String shipperPhone) {
        List<ShippingOrderModel> tempList = new ArrayList<>();
        Query orderRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPING_ORDER_REF)
                .orderByChild("shipperPhone")
                .equalTo(Common.currentShipperUser.getPhone());
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot orderSnapshot:dataSnapshot.getChildren())
                {
                    ShippingOrderModel shippingOrderModel = orderSnapshot.getValue(ShippingOrderModel.class);
                    shippingOrderModel.setKey(orderSnapshot.getKey());
                    tempList.add(shippingOrderModel);
                }
                listener.onShippingOrderLoadSuccess(tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onShippingOrderLoadFailed(databaseError.getMessage());
            }
        });
    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }

    @Override
    public void onShippingOrderLoadSuccess(List<ShippingOrderModel> shippingOrderModelList) {
        shippingOrderMutableLiveData.setValue(shippingOrderModelList);
    }

    @Override
    public void onShippingOrderLoadFailed(String message) {
        messageError.setValue(message);
    }
}