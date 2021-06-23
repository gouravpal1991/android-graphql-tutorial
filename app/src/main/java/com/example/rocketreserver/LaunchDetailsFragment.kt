package com.example.rocketreserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.api.load
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.example.rocketreserver.databinding.LaunchDetailsFragmentBinding
import java.lang.Exception

class LaunchDetailsFragment : Fragment() {

    private lateinit var binding: LaunchDetailsFragmentBinding
    val args: LaunchDetailsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LaunchDetailsFragmentBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            binding.bookButton.visibility = View.GONE
            binding.bookProgressBar.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            binding.error.visibility= View.GONE

            val response = try {
                apolloClient(requireContext()).query(LaunchDetailsQuery(id = args.launchId)).await()
            }catch (e:ApolloException){
                binding.progressBar.visibility = View.VISIBLE
                binding.error.text = "oh no...protocol error happened!"
                binding.error.visibility= View.VISIBLE
                return@launchWhenCreated
            }

            val launch = response.data?.launch
            if(launch ==null || response.hasErrors()){
                binding.progressBar.visibility = View.GONE
                binding.error.text = response.errors?.get(0)?.message
                binding.error.visibility= View.VISIBLE
                return@launchWhenCreated
            }

            binding.progressBar.visibility = View.GONE
            binding.error.text= ""
            binding.error.visibility = View.GONE

            binding.missionPatch.load(launch.mission?.missionPatch){
                placeholder(R.drawable.ic_placeholder)
            }
            binding.site.text = launch.site
            binding.missionName.text = launch.mission?.name
            val rocket = launch.rocket
            binding.rocketName.text = "🚀 ${rocket?.name} ${rocket?.type}"


            val mutation = if(launch.isBooked){
                CancelTripMutation(id = args.launchId)
            }else{
                BookTripMutation(id = args.launchId)
            }

            val res = try {
                apolloClient(requireContext()).mutate(mutation).await()
            }catch (e:ApolloException){
                configureButton(launch.isBooked)
                return@launchWhenCreated
            }

            if(res.hasErrors()){
                configureButton(launch.isBooked)
                return@launchWhenCreated
            }

            configureButton(!launch.isBooked)
        }
    }

    private fun configureButton(booked: Boolean) {
        binding.bookButton.visibility = View.VISIBLE
        binding.bookProgressBar.visibility= View.GONE

        binding.bookButton.text = if(booked){
            getString(R.string.cancel)
        }else{
            getString(R.string.book_now)
        }

        binding.bookButton.setOnClickListener {
            val context = context
            if(context!=null && User.getToken(context) == null){
                findNavController().navigate(R.id.open_login)
                return@setOnClickListener
            }
        }
    }
}