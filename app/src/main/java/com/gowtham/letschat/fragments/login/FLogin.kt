package com.gowtham.letschat.fragments.login

import android.content.Context
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.FLoginBinding
import com.gowtham.letschat.models.Country
import com.gowtham.letschat.ui.activities.SharedViewModel
import com.gowtham.letschat.utils.*
import com.gowtham.letschat.views.CustomProgressView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FLogin : Fragment() {

    private var country: Country? = null

    private lateinit var binding: FLoginBinding

    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private var progressView: CustomProgressView?=null

    private val viewModel by activityViewModels<LogInViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = FLoginBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressView = CustomProgressView(requireContext())
        setDataInView()
        subscribeObservers()
    }

    private fun setDataInView() {
        binding.viewmodel = viewModel
        setDefaultCountry()
        binding.txtCountryCode.setOnClickListener {
            Utils.closeKeyBoard(requireActivity())
            findNavController().navigate(R.id.action_FLogIn_to_FCountries)
        }
        binding.btnGetOtp.setOnClickListener {
            validate()
        }
    }

    private fun validate() {
        try {
            Utils.closeKeyBoard(requireActivity())
            val mobileNo = viewModel.mobile.value?.trim()
            val country = viewModel.country.value
            when {
                Validator.isMobileNumberEmpty(mobileNo) -> snack(requireActivity(), "Enter valid mobile number")
                country == null -> snack(requireActivity(), "Select a country")
                !Validator.isValidNo(country.code, mobileNo!!) -> snack(
                    requireActivity(),
                    "Enter valid mobile number"
                )
                Utils.isNoInternet(requireContext()) -> snackNet(requireActivity())
                else -> {
                    viewModel.setMobile()
                    viewModel.setProgress(true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setDefaultCountry() {
        try {
            country = Utils.getDefaultCountry()
            val manager =
                requireActivity().getSystemService(Context.TELEPHONY_SERVICE) as (TelephonyManager)?
            manager?.let {
                val countryCode = Utils.clearNull(manager.networkCountryIso)
                if (countryCode.isEmpty())
                    return
                val countries = Countries.getCountries()
                for (i in countries) {
                    if (i.code.equals(countryCode, true))
                        country = i
                }
                viewModel.setCountry(country!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun subscribeObservers() {
        try {
            sharedViewModel.country.observe(viewLifecycleOwner, {
                viewModel.setCountry(it)
            })

            viewModel.getProgress().observe(viewLifecycleOwner, {
                progressView?.toggle(it)
            })

            viewModel.getVerificationId().observe(viewLifecycleOwner, { vCode ->
                vCode?.let {
                    viewModel.setProgress(false)
                    viewModel.resetTimer()
                    viewModel.setVCodeNull()
                    viewModel.setEmptyText()
                    if (findNavController().isValidDestination(R.id.FLogIn))
                    findNavController().navigate(R.id.action_FLogIn_to_FVerify)
                }
            })

            viewModel.getFailed().observe(viewLifecycleOwner, {
                progressView?.dismiss()
            })

            viewModel.getTaskResult().observe(viewLifecycleOwner, { taskId ->
                if (taskId!=null && viewModel.getCredential().value?.smsCode.isNullOrEmpty())
                    viewModel.fetchUser(taskId)
            })

            viewModel.userProfileGot.observe(viewLifecycleOwner, { success ->
                if (success && viewModel.getCredential().value?.smsCode.isNullOrEmpty()
                               && findNavController().isValidDestination(R.id.FLogIn)) {
                    requireActivity().toastLong("Authenticated successfully using Instant verification")
                    findNavController().navigate(R.id.action_FLogIn_to_FProfile)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    /*       val action = FMobileDirections.actionFMobileToFVerify(
             Country(
                 code = "sd",
                 name = "sda",
                 noCode = "+83",
                 money = "mon"
             )
         )
         findNavController().navigate(action)*/

    override fun onDestroy() {
        try {
            progressView?.dismissIfShowing()
            super.onDestroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}