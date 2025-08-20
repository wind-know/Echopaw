package com.example.echopaw.callback

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.LocationSource
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.GeocodeQuery
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.example.echopaw.R
import com.example.echopaw.databinding.FragmentMapBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout

class MapFragment : Fragment(), AMapLocationListener, LocationSource,
    PoiSearch.OnPoiSearchListener, AMap.OnMapLongClickListener,
    AMap.OnMapClickListener, GeocodeSearch.OnGeocodeSearchListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val TAG = "MapFragment"

    // Location related
    private var mLocationClient: AMapLocationClient? = null
    private var mLocationOption: AMapLocationClientOption? = null
    private var aMap: AMap? = null
    private var mListener: LocationSource.OnLocationChangedListener? = null

    // Search related
    private var geocodeSearch: GeocodeSearch? = null
    private val PARSE_SUCCESS_CODE = 1000
    private var city: String? = null
    private var cityCode: String? = null
    private val markerList = mutableListOf<Marker>()
    private var adcode: String? = null
    private var location: String? = null
    private var addresses: List<String> = emptyList()

    // UI related
    private var autoTransition: AutoTransition? = null
    private var bigShowAnim: Animation? = null
    private var smallHideAnim: Animation? = null
    private var width = 0
    private var isOpen = false
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    // Interface for UI updates
    interface OnLocationDataListener {
        fun onLocationDataUpdated(location: String, city: String, adcode: String)
        fun onAddressSelected(address: String, latLng: LatLng)
    }

    private var locationDataListener: OnLocationDataListener? = null

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        Log.d(TAG, "Permission request result: $result")
        if (result) {
            Log.d(TAG, "Permission granted")
            showMsg("Permission granted")
        } else {
            Log.d(TAG, "Permission denied")
            showMsg("Permission denied")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 设置状态栏透明
        makeStatusBarTransparent()

        // 调整布局以避免状态栏覆盖内容
        adjustForKeyboard()
        binding.mapView.onCreate(savedInstanceState)
        initLocation()
        initMap()
        initView()
        initSearch()

        ViewCompat.setOnApplyWindowInsetsListener(binding.map) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }

    private fun makeStatusBarTransparent() {
        activity?.window?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            }
        }
    }

    private fun adjustForKeyboard() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun initLocation() {
        try {
            mLocationClient = AMapLocationClient(requireContext())
            mLocationClient?.setLocationListener(this)
            mLocationOption = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocationLatest = true
                isNeedAddress = true
                httpTimeOut = 6000
            }
            mLocationClient?.setLocationOption(mLocationOption)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun initMap() {
        if (aMap == null) {
            aMap = binding.mapView.map.apply {
                mapType = AMap.MAP_TYPE_NIGHT
                setLocationSource(this@MapFragment)
                isMyLocationEnabled = true
                moveCamera(CameraUpdateFactory.zoomTo(15f))
                showIndoorMap(true)

                setOnMapClickListener(this@MapFragment)
                setOnMapLongClickListener(this@MapFragment)

                uiSettings.apply {
                    isZoomControlsEnabled = false
                    isScaleControlsEnabled = true
                }
            }
        }
    }

    private fun initView() {
        val metrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(metrics)
        width = metrics.widthPixels
        bigShowAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_big_expand)
        smallHideAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_small_close)
        // 在 onViewCreated 或 onCreate 中
        binding.fabWorld.setOnClickListener {
//                showMsg("世界地图")
            // 方式2：带动画的跳转（推荐）
            val intent = Intent(requireContext(), WorldActivity::class.java).apply {
                putExtra("key_origin", "map_fragment")  // 可传递参数
            }
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        binding.tabLayout.apply {
            // 首先清除XML中定义的Tab
            removeAllTabs()
            // 设置Tab文本样式
            setTabTextColors(ContextCompat.getColor(context, R.color.home_gray),
                ContextCompat.getColor(context, R.color.white))

            // 设置指示条宽度（约为文本宽度的60%）
            post {
                for (i in 0 until tabCount) {
                    val tab = getTabAt(i)
                    val tabView = (tab?.view as? LinearLayout)?.getChildAt(0) as? TextView
                    tabView?.let {
                        val width = it.paint.measureText(it.text.toString())
                        val params = it.layoutParams as ViewGroup.MarginLayoutParams
                        params.width = (width * 0.6).toInt()
                        it.layoutParams = params
                    }
                }
            }
            // 为每个 Tab 设置自定义视图（确保是 TextView）
            repeat(3) { position ->
                val tab = newTab()
                val textView = TextView(context).apply {
                    text = when (position) {
                        0 -> "附近"
                        1 -> "热门"
                        else -> "最新"
                    }
                    setTextColor(ContextCompat.getColor(context, R.color.home_gray))
                    textSize = 14f
                    gravity = Gravity.CENTER
                }
                tab.customView = textView
                addTab(tab)
            }

            // 添加监听器（现在可以安全转换，因为 customView 是 TextView）
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    (tab.customView as? TextView)?.apply {
                        setTextColor(ContextCompat.getColor(context, R.color.white))
                        typeface = Typeface.DEFAULT_BOLD
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    (tab.customView as? TextView)?.apply {
                        setTextColor(ContextCompat.getColor(context, R.color.home_gray))
                        typeface = Typeface.DEFAULT
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetRay).apply {
            isHideable = true
            state = BottomSheetBehavior.STATE_COLLAPSED
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_COLLAPSED -> {

                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            // Hide search layout when bottom sheet is expanded
                            if (isOpen) {
                                initClose()
                            }
                        }
                        BottomSheetBehavior.STATE_DRAGGING -> {
                            // Handle dragging state if needed
                        }
                        BottomSheetBehavior.STATE_SETTLING -> {
                            // Handle settling state if needed
                        }
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            // Handle hidden state if needed
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Handle slide events if needed
                }
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.bottomSheetRay.elevation = 12f  // 确保高于地图
            }
        }
    }

    private fun initSearch() {
        try {
            geocodeSearch = GeocodeSearch(requireContext()).apply {
                setOnGeocodeSearchListener(this@MapFragment)
            }
        } catch (e: AMapException) {
            e.printStackTrace()
        }
    }

    private fun dip2px(dpVale: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpVale * scale + 0.5f).toInt()
    }

    private fun px2dip(pxValue: Float): Int {
        val scale = requireContext().resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun beginDelayedTransition(view: ViewGroup) {
        autoTransition = AutoTransition().apply {
            duration = 200
        }
        TransitionManager.beginDelayedTransition(view, autoTransition)
    }

    private fun initExpand() {
        if (context != null) {
            isOpen = true
//            binding.edSearch.visibility = View.VISIBLE
//            binding.ivClose.visibility = View.VISIBLE
//
//            (binding.laySearch.layoutParams as? LinearLayout.LayoutParams)?.apply {
//                // Convert width from pixels to dips, subtract 24 dips, then back to pixels
//                val currentWidthDips = px2dip(width.toFloat())
//                val newWidthDips = currentWidthDips - 24f  // Make sure we subtract a Float
//                width = dip2px(newWidthDips)
//
//                setMargins(0, 0, 0, 0)
//            }

//            binding.laySearch.setPadding(14, 0, 14, 0)
//            beginDelayedTransition(binding.laySearch)
        }
    }

    private fun initClose() {
        if (context != null) {
            isOpen = false
//            binding.edSearch.visibility = View.GONE
//            binding.edSearch.text?.clear()
//            binding.ivClose.visibility = View.GONE
//            (binding.laySearch.layoutParams as? LinearLayout.LayoutParams)?.apply {
//                width = dip2px(48f)
//                height = dip2px(48f)
//                setMargins(0, 0, 0, 0)
//            }
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireActivity().window.decorView.windowToken, 0)
//            beginDelayedTransition(binding.laySearch)
        }
    }

    private fun performSearch(address: String) {
        if (address.isEmpty()) {
            showMsg("Please enter an address")
        } else {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireActivity().window.decorView.windowToken, 0)
            geocodeSearch?.getFromLocationNameAsyn(GeocodeQuery(address, null))
        }
    }

    private fun startLocation() {
        mLocationClient?.startLocation()
    }

    private fun stopLocation() {
        mLocationClient?.stopLocation()
    }

    private fun showMsg(message: CharSequence) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onLocationChanged(aMapLocation: AMapLocation?) {
        aMapLocation ?: run {
            showMsg("Location failed, aMapLocation is null")
            return
        }

        if (aMapLocation.errorCode == 0) {
            stopLocation()
            mListener?.onLocationChanged(aMapLocation)
            city = aMapLocation.city
            adcode = aMapLocation.adCode
            location = "%.2f,%.2f".format(aMapLocation.longitude, aMapLocation.latitude)

            // Notify listener about new location data
            locationDataListener?.onLocationDataUpdated(
                location ?: "",
                city ?: "",
                adcode ?: ""
            )
        } else {
            showMsg("Location failed, error: ${aMapLocation.errorInfo}")
            Log.e(TAG, "location Error, ErrCode:${aMapLocation.errorCode}, errInfo:${aMapLocation.errorInfo}")
        }
    }

    override fun activate(listener: LocationSource.OnLocationChangedListener?) {
        mListener = listener
        startLocation()
    }

    override fun deactivate() {
        mListener = null
        mLocationClient?.stopLocation()
        mLocationClient?.onDestroy()
        mLocationClient = null
    }

    override fun onPoiSearched(poiResult: PoiResult?, i: Int) {
        poiResult?.pois?.forEach {
            Log.d("MapFragment", " Title:${it.title} Snippet:${it.snippet}")
        }
    }

    override fun onMapLongClick(p0: LatLng?) {
        if (p0 != null) {
            latLonToAddress(p0)
            updateMapmark(p0)
        }
    }

    override fun onMapClick(p0: LatLng?) {
        if (p0 != null) {
            latLonToAddress(p0)
            updateMapmark(p0)
        }
    }
    override fun onPoiItemSearched(p0: PoiItem?, p1: Int) {
        TODO("Not yet implemented")
    }

    private fun latLonToAddress(latLng: LatLng) {
        location = "${latLng.longitude},${latLng.latitude}"
        geocodeSearch?.getFromLocationAsyn(
            RegeocodeQuery(
                LatLonPoint(latLng.latitude, latLng.longitude),
                20f,
                GeocodeSearch.AMAP
            )
        )
    }


    private fun updateMapCenter(latLng: LatLng) {
        aMap?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition(latLng, 16f, 30f, 0f)
            )
        )
    }

    private fun updateMapmark(latLng: LatLng) {
        aMap?.clear()
        aMap?.addMarker(MarkerOptions().position(latLng).snippet("DefaultMarker"))
        updateMapCenter(latLng)
    }

    override fun onRegeocodeSearched(result: RegeocodeResult?, rCode: Int) {
        Log.e("GeoSearch", "rCode = $rCode")
        if (rCode == PARSE_SUCCESS_CODE) {
            result?.regeocodeAddress?.let { address ->
                val query = result.regeocodeQuery
                val point = query.point
                showMsg("Address: ${address.formatAddress}")
                Log.e("GeoSearch", "rCode = $rCode")
                locationDataListener?.onAddressSelected(
                    address.formatAddress ?: "",
                    LatLng(point.latitude, point.longitude)
                )
            }
        } else {
            showMsg("Failed to get address, rCode = $rCode")
        }
    }


    override fun onGeocodeSearched(geocodeResult: GeocodeResult?, rCode: Int) {
        if (rCode != PARSE_SUCCESS_CODE) {
            showMsg("Failed to get coordinates")
            return
        }

        geocodeResult?.geocodeAddressList?.firstOrNull()?.let {
            val latLonPoint = it.latLonPoint
            location = "${latLonPoint.longitude},${latLonPoint.latitude}"
            adcode = it.adcode

            // Update map marker
            updateMapmark(LatLng(latLonPoint.latitude, latLonPoint.longitude))
            if (isOpen) initClose()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "onResume: Permission already granted")
            startLocation()
        } else {
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
    }

    // Setter for location data listener
    fun setOnLocationDataListener(listener: OnLocationDataListener) {
        this.locationDataListener = listener
    }

}