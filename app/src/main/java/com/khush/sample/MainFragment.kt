package com.khush.sample

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.ketch.DownloadModel
import com.ketch.Ketch
import com.ketch.Status
import com.khush.sample.databinding.FragmentMainBinding
import com.khush.sample.databinding.ItemFileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File


class MainFragment : Fragment() {

    private lateinit var fragmentMainBinding: FragmentMainBinding
    private lateinit var adapter: FilesAdapter
    private lateinit var ketch: Ketch

    companion object {
        fun newInstance(): MainFragment {
            val args = Bundle()
            val fragment = MainFragment()
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
        ketch = (requireContext().applicationContext as MainApplication).ketch
        observer()
        fragmentMainBinding = FragmentMainBinding.inflate(inflater)
        return fragmentMainBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = FilesAdapter(object : FilesAdapter.FileClickListener {
            override fun onFileClick(downloadItem: DownloadModel) {
                if (downloadItem.status == Status.SUCCESS) {
                    val file = File(downloadItem.path, downloadItem.fileName)
                    if (file.exists()) {
                        val uri = this@MainFragment.context?.applicationContext?.let {
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
                    } else {
                        Toast.makeText(
                            this@MainFragment.context,
                            "Something went wrong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onCancelClick(downloadItem: DownloadModel) {
                ketch.cancel(downloadItem.id)
            }

            override fun onDownloadClick(downloadItem: DownloadModel) {
                ketch.download(
                    url = downloadItem.url,
                    fileName = downloadItem.fileName,
                    path = downloadItem.path,
                    tag = downloadItem.tag,
                    metaData = downloadItem.metaData
                )
            }

            override fun onPauseClick(downloadItem: DownloadModel) {
                ketch.pause(downloadItem.id)
            }

            override fun onResumeClick(downloadItem: DownloadModel) {
                ketch.resume(downloadItem.id)
            }

            override fun onRetryClick(downloadItem: DownloadModel) {
                ketch.retry(downloadItem.id)
            }

            override fun onDeleteClick(downloadItem: DownloadModel) {
                ketch.clearDb(downloadItem.id)
            }
        })
        fragmentMainBinding.recyclerView.adapter = adapter
        (fragmentMainBinding.recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        fragmentMainBinding.recyclerView.layoutManager =
            LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        fragmentMainBinding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                this.context,
                DividerItemDecoration.VERTICAL
            )
        )

        fragmentMainBinding.bt1.text = "Video 1"
        fragmentMainBinding.bt1.setOnClickListener {
            ketch.download(
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                fileName = "Sample_Video_1.mp4",
                tag = "Video",
                metaData = "158",
                customTitle = "Video 1 File"
            )
        }

        fragmentMainBinding.bt2.text = "Video 2"
        fragmentMainBinding.bt2.setOnClickListener {
            ketch.download(
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                fileName = "Sample_Video_2.mp4",
                tag = "Video",
                metaData = "169"
            )
        }

        fragmentMainBinding.bt3.text = "Video 3"
        fragmentMainBinding.bt3.setOnClickListener {
            ketch.download(
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                fileName = "Sample_Video_3.mp4",
                tag = "Video",
                metaData = "48"
            )
        }

        fragmentMainBinding.bt4.text = "Image 1"
        fragmentMainBinding.bt4.setOnClickListener {
            ketch.download(
                url = "https://picsum.photos/200/300",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                fileName = "Sample_Image_1.jpg",
                tag = "Document",
                metaData = "1"
            )
        }

        fragmentMainBinding.bt5.text = "Pdf 1"
        fragmentMainBinding.bt5.setOnClickListener {
            ketch.download(
                url = "https://sample-videos.com/pdf/Sample-pdf-5mb.pdf",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                fileName = "Sample_Pdf_1.pdf",
                tag = "Document",
                metaData = "5"
            )
        }

        fragmentMainBinding.bt6.text = "Multiple"
        fragmentMainBinding.bt6.setOnClickListener {
            ketch.download(
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                fileName = "Sample_Video_1.mp4",
                tag = "Video",
                metaData = "158"
            )
            ketch.download(
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                fileName = "Sample_Video_2.mp4",
                tag = "Video",
                metaData = "169"
            )
            ketch.download(
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                fileName = "Sample_Video_3.mp4",
                tag = "Video",
                metaData = "48"
            )
            ketch.download(
                url = "https://sample-videos.com/video321/mp4/720/big_buck_bunny_720p_30mb.mp4",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                fileName = "Sample_Video_4.mp4",
                tag = "Video",
                metaData = "30"
            )
            ketch.download(
                url = "https://picsum.photos/200/300",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                fileName = "Sample_Image_1.jpg",
                tag = "Document",
                metaData = "1"
            )
            ketch.download(
                url = "https://sample-videos.com/pdf/Sample-pdf-5mb.pdf",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                fileName = "Sample_Pdf_1.pdf",
                tag = "Document",
                metaData = "5",
            )
        }
    }

    private fun observer() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ketch.observeDownloads()
                    .flowOn(Dispatchers.IO)
                    .collect {
                        adapter.submitList(it)
                    }
            }
        }
    }
}


class FilesAdapter(private val listener: FileClickListener) :
    ListAdapter<DownloadModel, FilesAdapter.ViewHolder>(
        DiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(downloadModel: DownloadModel) {
            binding.fileName.text = downloadModel.fileName
            binding.status.text = downloadModel.status.toString()
            binding.progressBar.progress = downloadModel.progress
            binding.progressText.text =
                downloadModel.progress.toString() + "%/" + Util.getTotalLengthText(downloadModel.total) + ", "
            binding.size.text = Util.getCompleteText(
                downloadModel.speedInBytePerMs,
                downloadModel.progress,
                downloadModel.total
            )

            binding.downloadButton.setOnClickListener {
                listener.onDownloadClick(downloadModel)
            }
            binding.cancelButton.setOnClickListener {
                listener.onCancelClick(downloadModel)
            }
            binding.pauseButton.setOnClickListener {
                listener.onPauseClick(downloadModel)
            }
            binding.resumeButton.setOnClickListener {
                listener.onResumeClick(downloadModel)
            }
            binding.deleteButton.setOnClickListener {
                listener.onDeleteClick(downloadModel)
            }
            binding.retryButton.setOnClickListener {
                listener.onRetryClick(downloadModel)
            }
            binding.root.setOnClickListener {
                listener.onFileClick(downloadModel)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DownloadModel>() {
        override fun areItemsTheSame(oldItem: DownloadModel, newItem: DownloadModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DownloadModel, newItem: DownloadModel): Boolean {
            return (oldItem == newItem)
        }

    }

    interface FileClickListener {
        fun onFileClick(downloadItem: DownloadModel)
        fun onCancelClick(downloadItem: DownloadModel)
        fun onDownloadClick(downloadItem: DownloadModel)
        fun onPauseClick(downloadItem: DownloadModel)
        fun onResumeClick(downloadItem: DownloadModel)
        fun onRetryClick(downloadItem: DownloadModel)
        fun onDeleteClick(downloadItem: DownloadModel)
    }

}