package com.linkme.fido.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

/**
 * 모든 Fragment가 상속받을 BaseFragment...
 * 아직 쓸데는 없음... 공통 처리 로직이 생기면 그때 구현
 */
public class BaseFragment extends Fragment {
    public BaseFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}