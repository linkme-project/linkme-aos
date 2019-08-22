package com.linkme.fido.fido;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.linkme.fido.R;
import com.linkme.fido.activity.MainActivity;


public class Utilities {

    private final static String TAG = Utilities.class.getSimpleName();

    public static View getLayoutInflater(Context context, int layout) {
        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(layout, null);
    }

    public static void callFragment(final MainActivity acitivity, final Fragment fragment, final Bundle bundle, final boolean setAnimation,
                                    final boolean isSelfStart, boolean isBack) {

        if (Utilities.isActivityFinishied(acitivity) || fragment == null) {

            return;
        }


        FragmentManager fragmentManager = acitivity.getSupportFragmentManager();

        if (fragment != null) {

            if (bundle != null) {
                fragment.setArguments(bundle);
            }

            try {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                if (setAnimation && !isBack) {
                    fragmentTransaction.setCustomAnimations(R.anim.fragment_in, R.anim.fragment_out, R.anim.fragment_in, R.anim.fragment_out);
                } else if (setAnimation && isBack) {
                    fragmentTransaction.setCustomAnimations(R.anim.fragment_back_out, R.anim.fragment_back_in, R.anim.fragment_back_in, R.anim.fragment_back_out);
                }
//                fragmentTransaction.replace(R.id.container, fragment);

                if(isSelfStart){
                    fragmentTransaction.add(R.id.main_container, fragment);
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                        fragmentTransaction.commit();
                    } else {
                        fragmentTransaction.commit();
                        fragmentManager.executePendingTransactions();
                    }
                } else {
                    fragmentTransaction.replace(R.id.main_container, fragment);
                    fragmentTransaction.commit();
                }

            } catch (IllegalStateException e) {
                if(!acitivity.isFinishing()) {
                    fragmentManager.beginTransaction().replace(R.id.main_container, fragment).commitAllowingStateLoss();
                    e.printStackTrace();
                }
            }
        }
    }

    public static void callFragment(MainActivity acitivity, Fragment fragment, Bundle bundle) {
        callFragment(acitivity, fragment, bundle, true, false,false);
    }

    public static void callFragment(MainActivity acitivity, Fragment fragment, Bundle bundle, boolean setAnimation) {
        callFragment(acitivity, fragment, bundle, setAnimation, false,false);
    }

    public static void callFragmentBack(MainActivity acitivity, Fragment fragment, Bundle bundle, boolean setAnimation) {
        callFragment(acitivity, fragment, bundle, setAnimation, false,true);
    }


    public static void addFragment(MainActivity acitivity, Fragment fragment, Bundle bundle, boolean setAnimation) {
        addFragment(acitivity, fragment, bundle, setAnimation, null);
    }

    public static void addFragment(MainActivity acitivity, Fragment fragment, Bundle bundle, boolean setAnimation, String tag) {
        FragmentManager fragmentManager = acitivity.getSupportFragmentManager();

        if (fragment != null) {
            if (bundle != null) {
                fragment.setArguments(bundle);
            }

            try {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                if (setAnimation) {
                    fragmentTransaction.setCustomAnimations(R.anim.fragment_in, R.anim.fragment_out, R.anim.fragment_in, R.anim.fragment_out);
                }

                if(Utilities.isEmpty(tag)) {
                    fragmentTransaction.add(R.id.main_container, fragment);
                } else {
                    fragmentTransaction.add(R.id.main_container, fragment, tag);
                }
                fragmentTransaction.addToBackStack(fragment.getClass().getName());
//                fragmentTransaction.commit();
                fragmentTransaction.commitAllowingStateLoss();
            } catch (IllegalStateException e) {
//                fragmentManager.beginTransaction().replace(R.id.container, fragment).commitAllowingStateLoss();
                e.printStackTrace();
            }
        }
    }

    public static boolean isEmpty(String str) {
        if (str == null || "".equals(str.trim()))
            return true;
        else
            return false;
    }

    public static boolean isActivityFinishied(MainActivity activity) {
        if(activity == null) {
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return activity.isFinishing() || activity.isDestroyed();
        } else {
            return activity.isFinishing();
        }
    }


}
