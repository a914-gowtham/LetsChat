package com.gowtham.letschat.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.gowtham.letschat.fragments.contacts.FContacts
import com.gowtham.letschat.utils.MPreference
import javax.inject.Inject

class MainFragmentFactory @Inject constructor(
private val preference: MPreference
) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when(className) {
            FContacts::class.java.name -> FContacts(preference)
            else -> super.instantiate(classLoader, className)
        }

    }
}