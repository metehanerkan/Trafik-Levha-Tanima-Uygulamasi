package com.example.mobil_uygulama.ui.home

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobil_uygulama.databinding.FragmentHomeBinding
import com.example.mobil_uygulama.ui.HelpActivity
import com.example.mobil_uygulama.ui.LogAdapter
import com.example.mobil_uygulama.ui.LogItem
import com.example.mobil_uygulama.ui.SharedLocationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sharedLocationViewModel: SharedLocationViewModel by activityViewModels()

    private val logList = mutableListOf<LogItem>()
    private lateinit var logAdapter: LogAdapter
    private lateinit var tts: TextToSpeech

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonHelp.setOnClickListener {
            val intent = Intent(requireContext(), HelpActivity::class.java)
            startActivity(intent)
        }

        logAdapter = LogAdapter(logList)
        binding.logRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.logRecyclerView.adapter = logAdapter

        // HIZ GÜNCELLEMESİ: XML'de anlık hızı devasa punto yaptık, yanındaki "km/h" sabit.
        // Bu yüzden sadece rakamı basıyoruz.
        sharedLocationViewModel.speedKmh.observe(viewLifecycleOwner) { speed ->
            binding.textSpeed.text = "%.1f".format(speed)
        }

        sharedLocationViewModel.lastDetection.observe(viewLifecycleOwner) { item ->
            val timestamp = getCurrentTime()
            val bitmap = decodeBase64(item.imageBase64) // UYARI: Bunu ileride arka plana taşı!

            // Veriyi kirletmeden, timestamp parametresini ayrı gönderiyoruz
            logList.add(0, LogItem(item.label, item.confidence, bitmap, timestamp))
            logAdapter.notifyItemInserted(0)

            // YENİ EKLENTİ: Yeni log geldiğinde liste otomatik olarak en başa kaysın ki kullanıcı görebilsin
            binding.logRecyclerView.scrollToPosition(0)
        }
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun decodeBase64(base64Str: String) = BitmapFactory.decodeByteArray(
        Base64.decode(base64Str.substringAfter(","), Base64.DEFAULT), 0,
        Base64.decode(base64Str.substringAfter(","), Base64.DEFAULT).size
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}