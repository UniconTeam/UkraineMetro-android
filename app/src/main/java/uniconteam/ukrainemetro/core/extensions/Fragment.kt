package uniconteam.ukrainemetro.core.extensions

import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import uniconteam.ukrainemetro.core.platform.BaseFragment

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) =
    beginTransaction().func().commit()

fun BaseFragment.close() = fragmentManager?.popBackStack()

val BaseFragment.appContext: Context get() = activity?.applicationContext!!