package com.faciletech.heed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.faciletech.heed.utils.KeyConstants
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TipsFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return when (arguments?.getInt(KeyConstants.TIP_TYPE)) {
            1 -> {
                inflater.inflate(R.layout.outside_tips, container, false)
            }
            2 -> {
                inflater.inflate(R.layout.inside_tips, container, false)
            }
            else -> {
                null
            }
        }
    }

}
