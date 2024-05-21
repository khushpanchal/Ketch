package com.khush.sample

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ketch.DownloadState
import com.ketch.Ketch
import com.ketch.NotificationConfig
import com.ketch.Request
import com.khush.sample.databinding.FragmentDownloadSamplesBinding
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("SetTextI18n")
class DownloadSamplesFragment : Fragment() {

    private lateinit var ketch: Ketch
    private lateinit var fragmentDownloadSamplesBinding: FragmentDownloadSamplesBinding

    private var request1: Request? = null
    private var request2: Request? = null
    private var request3: Request? = null
    private var request4: Request? = null
    private var request5: Request? = null

    private var length1Text: String = "0.00 b"
    private var length2Text: String = "0.00 b"
    private var length3Text: String = "0.00 b"
    private var length4Text: String = "0.00 b"
    private var length5Text: String = "0.00 b"

    companion object {
        fun newInstance(): DownloadSamplesFragment {
            val args = Bundle()
            val fragment = DownloadSamplesFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        ketch = Ketch.init(
            this.requireContext(),
            notificationConfig = NotificationConfig(
                enabled = true,
                smallIcon = R.drawable.ic_launcher_foreground
            )
        )
        fragmentDownloadSamplesBinding = FragmentDownloadSamplesBinding.inflate(inflater)
        return fragmentDownloadSamplesBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentDownloadSamplesBinding.cancelButton1.setOnClickListener {
            if (fragmentDownloadSamplesBinding.cancelButton1.text == "Cancel") {
                if (request1 != null) {
                    ketch.cancel(request1!!.id)
                }
            } else if (fragmentDownloadSamplesBinding.cancelButton1.text == "Open") {
                if (request1 != null) {
                    openFile(
                        this@DownloadSamplesFragment.context,
                        request1!!.path,
                        request1!!.fileName
                    )
                }
            } else if (fragmentDownloadSamplesBinding.cancelButton1.text == "Download") {
                download1()
                fragmentDownloadSamplesBinding.cancelButton1.text = "Cancel"
            }
        }

        fragmentDownloadSamplesBinding.cancelButton2.setOnClickListener {
            if (fragmentDownloadSamplesBinding.cancelButton2.text == "Cancel") {
                if (request2 != null) {
                    ketch.cancel(request2!!.id)
                }
            } else if (fragmentDownloadSamplesBinding.cancelButton2.text == "Open") {
                if (request2 != null) {
                    openFile(
                        this@DownloadSamplesFragment.context,
                        request2!!.path,
                        request2!!.fileName
                    )
                }
            } else if (fragmentDownloadSamplesBinding.cancelButton2.text == "Download") {
                download2()
                fragmentDownloadSamplesBinding.cancelButton2.text = "Cancel"
            }
        }

        fragmentDownloadSamplesBinding.cancelButton3.setOnClickListener {
            if (fragmentDownloadSamplesBinding.cancelButton3.text == "Cancel") {
                if (request3 != null) {
                    ketch.cancel(request3!!.id)
                }
            } else if (fragmentDownloadSamplesBinding.cancelButton3.text == "Open") {
                if (request3 != null) {
                    openFile(
                        this@DownloadSamplesFragment.context,
                        request3!!.path,
                        request3!!.fileName
                    )
                }
            } else if (fragmentDownloadSamplesBinding.cancelButton3.text == "Download") {
                download3()
                fragmentDownloadSamplesBinding.cancelButton3.text = "Cancel"
            }
        }

        fragmentDownloadSamplesBinding.cancelButton4.setOnClickListener {
            if (fragmentDownloadSamplesBinding.cancelButton4.text == "Cancel") {
                if (request4 != null) {
                    ketch.cancel(request4!!.id)
                }
            } else if (fragmentDownloadSamplesBinding.cancelButton4.text == "Open") {
                if (request4 != null) {
                    openFile(
                        this@DownloadSamplesFragment.context,
                        request4!!.path,
                        request4!!.fileName
                    )
                }
            } else if (fragmentDownloadSamplesBinding.cancelButton4.text == "Download") {
                download4()
                fragmentDownloadSamplesBinding.cancelButton4.text = "Cancel"
            }
        }

        fragmentDownloadSamplesBinding.cancelButton5.setOnClickListener {
            if (fragmentDownloadSamplesBinding.cancelButton5.text == "Cancel") {
                if (request5 != null) {
                    ketch.cancel(request5!!.id)
                }
            } else if (fragmentDownloadSamplesBinding.cancelButton5.text == "Open") {
                if (request5 != null) {
                    openFile(
                        this@DownloadSamplesFragment.context,
                        request5!!.path,
                        request5!!.fileName
                    )
                }
            } else if (fragmentDownloadSamplesBinding.cancelButton5.text == "Download") {
                download5()
                fragmentDownloadSamplesBinding.cancelButton5.text = "Cancel"
            }
        }
    }

    private fun download1() {
        val request = ketch.download(
            url = "https://file-examples.com/storage/fe4996602366316ffa06467/2017/04/file_example_MP4_640_3MG.mp4",
            fileName = "Sample_Video.mp4"
        ).also {
            request1 = it
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ketch.observeDownloadById(request.id).collect {
                    when (it) {
                        DownloadState.Blocked -> {}
                        DownloadState.Cancel -> {
                            fragmentDownloadSamplesBinding.status1.text =
                                Status.CANCELLED.toString()
                            fragmentDownloadSamplesBinding.progressBar1.progress = 0
                            fragmentDownloadSamplesBinding.progressText1.text = ""
                            fragmentDownloadSamplesBinding.size1.text = ""
                        }

                        is DownloadState.Error -> {
                            fragmentDownloadSamplesBinding.status1.text = Status.FAILED.toString()
                            fragmentDownloadSamplesBinding.progressText1.text = it.error
                            fragmentDownloadSamplesBinding.size1.text = ""
                        }

                        is DownloadState.Progress -> {
                            length1Text = Util.getTotalLengthText(it.length)

                            fragmentDownloadSamplesBinding.status1.text = Status.PROGRESS.toString()
                            fragmentDownloadSamplesBinding.progressBar1.progress = it.progress
                            fragmentDownloadSamplesBinding.progressText1.text =
                                "${it.progress}%/$length1Text, "
                            fragmentDownloadSamplesBinding.size1.text = Util.getTimeLeftText(
                                it.speedInBytePerMs,
                                it.progress,
                                it.length
                            ) + ", " + Util.getSpeedText(it.speedInBytePerMs)
                        }

                        DownloadState.Queued -> {
                            fragmentDownloadSamplesBinding.fileName1.text = request1?.fileName
                            fragmentDownloadSamplesBinding.status1.text = Status.QUEUED.toString()
                        }

                        is DownloadState.Started -> {
                            length1Text = Util.getTotalLengthText(it.length)
                            fragmentDownloadSamplesBinding.status1.text = Status.STARTED.toString()
                        }

                        DownloadState.Success -> {
                            fragmentDownloadSamplesBinding.status1.text = Status.SUCCESS.toString()
                            fragmentDownloadSamplesBinding.progressBar1.progress = 100
                            fragmentDownloadSamplesBinding.progressText1.text = "100%/$length1Text"
                            fragmentDownloadSamplesBinding.size1.text = ""
                            fragmentDownloadSamplesBinding.cancelButton1.text = "Open"
                        }
                    }
                }
            }
        }
    }

    private fun download2() {
        val request = ketch.download(
            url = "https://file-examples.com/storage/fe4996602366316ffa06467/2017/11/file_example_MP3_700KB.mp3",
            fileName = "Sample_Audio.mp3"
        ).also {
            request2 = it
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ketch.observeDownloadById(request.id).collect {
                    when (it) {
                        DownloadState.Blocked -> {}
                        DownloadState.Cancel -> {
                            fragmentDownloadSamplesBinding.status2.text =
                                Status.CANCELLED.toString()
                            fragmentDownloadSamplesBinding.progressBar2.progress = 0
                            fragmentDownloadSamplesBinding.progressText2.text = ""
                            fragmentDownloadSamplesBinding.size2.text = ""
                        }

                        is DownloadState.Error -> {
                            fragmentDownloadSamplesBinding.status2.text = Status.FAILED.toString()
                            fragmentDownloadSamplesBinding.progressText2.text = it.error
                            fragmentDownloadSamplesBinding.size2.text = ""
                        }

                        is DownloadState.Progress -> {
                            length2Text = Util.getTotalLengthText(it.length)

                            fragmentDownloadSamplesBinding.status2.text = Status.PROGRESS.toString()
                            fragmentDownloadSamplesBinding.progressBar2.progress = it.progress
                            fragmentDownloadSamplesBinding.progressText2.text =
                                "${it.progress}%/$length2Text, "
                            fragmentDownloadSamplesBinding.size2.text = Util.getTimeLeftText(
                                it.speedInBytePerMs,
                                it.progress,
                                it.length
                            ) + ", " + Util.getSpeedText(it.speedInBytePerMs)
                        }

                        DownloadState.Queued -> {
                            fragmentDownloadSamplesBinding.fileName2.text = request2?.fileName
                            fragmentDownloadSamplesBinding.status2.text = Status.QUEUED.toString()
                        }

                        is DownloadState.Started -> {
                            length2Text = Util.getTotalLengthText(it.length)
                            fragmentDownloadSamplesBinding.status2.text = Status.STARTED.toString()
                        }

                        DownloadState.Success -> {
                            fragmentDownloadSamplesBinding.status2.text = Status.SUCCESS.toString()
                            fragmentDownloadSamplesBinding.progressBar2.progress = 100
                            fragmentDownloadSamplesBinding.progressText2.text = "100%/$length2Text"
                            fragmentDownloadSamplesBinding.size2.text = ""
                            fragmentDownloadSamplesBinding.cancelButton2.text = "Open"
                        }
                    }
                }
            }
        }
    }

    private fun download3() {
        val request = ketch.download(
            url = "https://file-examples.com/storage/fe4996602366316ffa06467/2017/10/file-sample_150kB.pdf",
            fileName = "Sample_Doc.pdf"
        ).also {
            request3 = it
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ketch.observeDownloadById(request.id).collect {
                    when (it) {
                        DownloadState.Blocked -> {}
                        DownloadState.Cancel -> {
                            fragmentDownloadSamplesBinding.status3.text =
                                Status.CANCELLED.toString()
                            fragmentDownloadSamplesBinding.progressBar3.progress = 0
                            fragmentDownloadSamplesBinding.progressText3.text = ""
                            fragmentDownloadSamplesBinding.size3.text = ""
                        }

                        is DownloadState.Error -> {
                            fragmentDownloadSamplesBinding.status3.text = Status.FAILED.toString()
                            fragmentDownloadSamplesBinding.progressText3.text = it.error
                            fragmentDownloadSamplesBinding.size3.text = ""
                        }

                        is DownloadState.Progress -> {
                            length3Text = Util.getTotalLengthText(it.length)

                            fragmentDownloadSamplesBinding.status3.text = Status.PROGRESS.toString()
                            fragmentDownloadSamplesBinding.progressBar3.progress = it.progress
                            fragmentDownloadSamplesBinding.progressText3.text =
                                "${it.progress}%/$length3Text, "
                            fragmentDownloadSamplesBinding.size3.text = Util.getTimeLeftText(
                                it.speedInBytePerMs,
                                it.progress,
                                it.length
                            ) + ", " + Util.getSpeedText(it.speedInBytePerMs)
                        }

                        DownloadState.Queued -> {
                            fragmentDownloadSamplesBinding.fileName3.text = request3?.fileName
                            fragmentDownloadSamplesBinding.status3.text = Status.QUEUED.toString()
                        }

                        is DownloadState.Started -> {
                            length3Text = Util.getTotalLengthText(it.length)
                            fragmentDownloadSamplesBinding.status3.text = Status.STARTED.toString()
                        }

                        DownloadState.Success -> {
                            fragmentDownloadSamplesBinding.status3.text = Status.SUCCESS.toString()
                            fragmentDownloadSamplesBinding.progressBar3.progress = 100
                            fragmentDownloadSamplesBinding.progressText3.text = "100%/$length3Text"
                            fragmentDownloadSamplesBinding.size3.text = ""
                            fragmentDownloadSamplesBinding.cancelButton3.text = "Open"
                        }
                    }
                }
            }
        }
    }

    private fun download4() {
        val request = ketch.download(
            url = "https://file-examples.com/storage/fe4996602366316ffa06467/2017/10/file_example_JPG_100kB.jpg",
            fileName = "Sample_Image.jpg"
        ).also {
            request4 = it
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ketch.observeDownloadById(request.id).collect {
                    when (it) {
                        DownloadState.Blocked -> {}
                        DownloadState.Cancel -> {
                            fragmentDownloadSamplesBinding.status4.text =
                                Status.CANCELLED.toString()
                            fragmentDownloadSamplesBinding.progressBar4.progress = 0
                            fragmentDownloadSamplesBinding.progressText4.text = ""
                            fragmentDownloadSamplesBinding.size4.text = ""
                        }

                        is DownloadState.Error -> {
                            fragmentDownloadSamplesBinding.status4.text = Status.FAILED.toString()
                            fragmentDownloadSamplesBinding.progressText4.text = it.error
                            fragmentDownloadSamplesBinding.size4.text = ""
                        }

                        is DownloadState.Progress -> {
                            length4Text = Util.getTotalLengthText(it.length)

                            fragmentDownloadSamplesBinding.status4.text = Status.PROGRESS.toString()
                            fragmentDownloadSamplesBinding.progressBar4.progress = it.progress
                            fragmentDownloadSamplesBinding.progressText4.text =
                                "${it.progress}%/$length4Text, "
                            fragmentDownloadSamplesBinding.size4.text = Util.getTimeLeftText(
                                it.speedInBytePerMs,
                                it.progress,
                                it.length
                            ) + ", " + Util.getSpeedText(it.speedInBytePerMs)
                        }

                        DownloadState.Queued -> {
                            fragmentDownloadSamplesBinding.fileName4.text = request4?.fileName
                            fragmentDownloadSamplesBinding.status4.text = Status.QUEUED.toString()
                        }

                        is DownloadState.Started -> {
                            length4Text = Util.getTotalLengthText(it.length)
                            fragmentDownloadSamplesBinding.status4.text = Status.STARTED.toString()
                        }

                        DownloadState.Success -> {
                            fragmentDownloadSamplesBinding.status4.text = Status.SUCCESS.toString()
                            fragmentDownloadSamplesBinding.progressBar4.progress = 100
                            fragmentDownloadSamplesBinding.progressText4.text = "100%/$length4Text"
                            fragmentDownloadSamplesBinding.size4.text = ""
                            fragmentDownloadSamplesBinding.cancelButton4.text = "Open"
                        }
                    }
                }
            }
        }
    }

    private fun download5() {
        val request = ketch.download(
            url = "https://file-examples.com/storage/fe4996602366316ffa06467/2017/10/file_example_GIF_500kB.gif",
            fileName = "Sample_Gif.gif"
        ).also {
            request5 = it
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ketch.observeDownloadById(request.id).collect {
                    when (it) {
                        DownloadState.Blocked -> {}
                        DownloadState.Cancel -> {
                            fragmentDownloadSamplesBinding.status5.text =
                                Status.CANCELLED.toString()
                            fragmentDownloadSamplesBinding.progressBar5.progress = 0
                            fragmentDownloadSamplesBinding.progressText5.text = ""
                            fragmentDownloadSamplesBinding.size5.text = ""
                        }

                        is DownloadState.Error -> {
                            fragmentDownloadSamplesBinding.status5.text = Status.FAILED.toString()
                            fragmentDownloadSamplesBinding.progressText5.text = it.error
                            fragmentDownloadSamplesBinding.size5.text = ""
                        }

                        is DownloadState.Progress -> {
                            length5Text = Util.getTotalLengthText(it.length)

                            fragmentDownloadSamplesBinding.status5.text = Status.PROGRESS.toString()
                            fragmentDownloadSamplesBinding.progressBar5.progress = it.progress
                            fragmentDownloadSamplesBinding.progressText5.text =
                                "${it.progress}%/$length5Text, "
                            fragmentDownloadSamplesBinding.size5.text = Util.getTimeLeftText(
                                it.speedInBytePerMs,
                                it.progress,
                                it.length
                            ) + ", " + Util.getSpeedText(it.speedInBytePerMs)
                        }

                        DownloadState.Queued -> {
                            fragmentDownloadSamplesBinding.fileName5.text = request5?.fileName
                            fragmentDownloadSamplesBinding.status5.text = Status.QUEUED.toString()
                        }

                        is DownloadState.Started -> {
                            length5Text = Util.getTotalLengthText(it.length)
                            fragmentDownloadSamplesBinding.status5.text = Status.STARTED.toString()
                        }

                        DownloadState.Success -> {
                            fragmentDownloadSamplesBinding.status5.text = Status.SUCCESS.toString()
                            fragmentDownloadSamplesBinding.progressBar5.progress = 100
                            fragmentDownloadSamplesBinding.progressText5.text = "100%/$length5Text"
                            fragmentDownloadSamplesBinding.size5.text = ""
                            fragmentDownloadSamplesBinding.cancelButton5.text = "Open"
                        }
                    }
                }
            }
        }
    }

    private fun openFile(context: Context?, path: String, fileName: String) {
        val file = File(path, fileName)
        if (file.exists()) {
            val uri = context?.applicationContext?.let {
                FileProvider.getUriForFile(
                    it,
                    it.packageName + ".provider",
                    file
                )
            }
            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, requireContext().contentResolver.getType(uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(intent)
                } catch (ignore: Exception) {
                }
            }
        }
    }
}