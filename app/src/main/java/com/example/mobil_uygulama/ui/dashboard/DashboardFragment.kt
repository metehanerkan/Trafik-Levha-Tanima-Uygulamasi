package com.example.mobil_uygulama.ui.dashboard

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.mobil_uygulama.databinding.FragmentDashboardBinding
import com.example.mobil_uygulama.ui.SharedLocationViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // SharedViewModel (Activity bazlı - harita/hız verisi için ortak kullanım)
    private val sharedLocationViewModel: SharedLocationViewModel by activityViewModels()

    // Sayfaya özel ViewModel (Sadece istatistikler için)
    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Üstteki Özet Kartlarını Dolduruyoruz
        dashboardViewModel.totalSigns.observe(viewLifecycleOwner) { total ->
            binding.textTotalSigns.text = total.toString()
        }

        dashboardViewModel.avgConfidence.observe(viewLifecycleOwner) { avg ->
            binding.textAvgConfidence.text = "%${(avg * 100).toInt()}"
        }

        setupBarChart()
    }

    private fun setupBarChart() {
        // Dinamik Tema Renklerini Çekiyoruz (Gölgeleme hatası giderildi)
        val themeTextColor = getThemeColor(com.google.android.material.R.attr.colorOnSurface)
        val themePrimaryColor = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val themeGridColor = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant)

        sharedLocationViewModel.getDistances().observe(viewLifecycleOwner) { map ->
            val dayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val entries = dayOrder.mapIndexed { index, key ->
                BarEntry(index.toFloat(), map[key] ?: 0f)
            }

            val barDataSet = BarDataSet(entries, "Günlük Mesafe (km)").apply {
                color = themePrimaryColor
                valueTextSize = 12f
                valueTextColor = themeTextColor
            }

            val barData = BarData(barDataSet).apply {
                barWidth = 0.6f
            }

            binding.barChart.apply {
                data = barData
                setFitBars(true)
                description.isEnabled = false
                legend.textColor = themeTextColor
                animateY(800)

                axisLeft.apply {
                    axisMinimum = 0f
                    granularity = 1f
                    textColor = themeTextColor
                    gridColor = themeGridColor
                }

                axisRight.isEnabled = false

                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(
                        listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
                    )
                    granularity = 1f
                    setDrawGridLines(false)
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = themeTextColor
                    textSize = 12f
                }

                invalidate() // Grafiği yenile
            }
        }
    }

    private fun setupPieChart() {
        val themeTextColor = getThemeColor(com.google.android.material.R.attr.colorOnSurface)

        // Şimdilik arayüzü test etmek için örnek (mock) veri giriyoruz.
        // İleride bunları SharedLocationViewModel içinden çekeceksin.
        val entries = ArrayList<com.github.mikephil.charting.data.PieEntry>()
        entries.add(com.github.mikephil.charting.data.PieEntry(45f, "Hız Sınırı"))
        entries.add(com.github.mikephil.charting.data.PieEntry(25f, "Dur"))
        entries.add(com.github.mikephil.charting.data.PieEntry(15f, "Yaya Geçidi"))
        entries.add(com.github.mikephil.charting.data.PieEntry(15f, "Diğer"))

        val dataSet = com.github.mikephil.charting.data.PieDataSet(entries, "")

        // MPAndroidChart'ın içindeki hazır şık renk paletini kullanıyoruz
        dataSet.colors = com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = android.graphics.Color.WHITE
        dataSet.valueTextSize = 14f
        dataSet.sliceSpace = 3f // Dilimler arası estetik boşluk
        dataSet.selectionShift = 5f

        val data = com.github.mikephil.charting.data.PieData(dataSet)

        binding.pieChart.apply {
            this.data = data
            description.isEnabled = false
            legend.textColor = themeTextColor
            legend.isWordWrapEnabled = true
            isDrawHoleEnabled = true // Ortasını delik yapar (Donut chart görünümü)
            setHoleColor(android.graphics.Color.TRANSPARENT)
            setCenterText("Levhalar")
            setCenterTextColor(themeTextColor)
            animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
            invalidate()
        }
    }

    // Temadan renk çeken yardımcı fonksiyon
    private fun getThemeColor(attrId: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrId, typedValue, true)
        return typedValue.data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Binding'i temizle (Memory leak - bellek sızıntısını önlemek için kritik)
        _binding = null
    }
}