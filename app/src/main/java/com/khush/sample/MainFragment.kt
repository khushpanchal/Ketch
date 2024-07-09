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
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID


class MainFragment : Fragment() {

    private lateinit var fragmentMainBinding: FragmentMainBinding
    private lateinit var adapter: FilesAdapter

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
                Ketch.getInstance(context!!).cancel(downloadItem.id)
            }

            override fun onDownloadClick(downloadItem: DownloadModel) {
                Ketch.getInstance(context!!).download(
                    url = downloadItem.url,
                    fileName = downloadItem.fileName,
                    tag = downloadItem.tag
                )
            }

            override fun onPauseClick(downloadItem: DownloadModel) {
                Ketch.getInstance(context!!).pause(downloadItem.id)
            }

            override fun onResumeClick(downloadItem: DownloadModel) {
                Ketch.getInstance(context!!).resume(downloadItem.id)
            }

            override fun onRetryClick(downloadItem: DownloadModel) {
                Ketch.getInstance(context!!).retry(downloadItem.id)
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
        fragmentMainBinding.button.setOnClickListener {
            val url = fragmentMainBinding.editTextUrl.text.toString()
            val fileName = fragmentMainBinding.editTextName.text.toString()
            if (url.isEmpty()) {
                Toast.makeText(this.context, "Enter Valid URL", Toast.LENGTH_SHORT).show()
            } else if (fileName.isEmpty()) {
                Toast.makeText(this.context, "Enter Valid File name", Toast.LENGTH_SHORT).show()
            } else {
                Ketch.getInstance(requireContext()).download(url = url, fileName = fileName)
            }
        }

        val testList = listOf(DownloadModel(
            url = "https://sample-videos.com/video321/mp4/720/big_buck_bunny_720p_30mb.mp4",
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
            fileName = "Sample_Video.mp4",
            tag = "my downloads",
            id = -1,
            status = Status.DEFAULT,
            timeQueued = 0L,
            progress = 0,
            total = 0L,
            speedInBytePerMs = 0f,
            headers = hashMapOf(),
            uuid = UUID.randomUUID(),
            eTag = ""
        ))

        adapter.submitList(testList)

    }

    private fun observer() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Ketch.getInstance(requireContext()).observeDownloads()
                    .collect {//observe from viewModel to survive configuration change
                        if(it.isNotEmpty()) {
                            adapter.submitList(it)
                        }
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
            binding.size.text = Util.getTimeLeftText(
                downloadModel.speedInBytePerMs,
                downloadModel.progress,
                downloadModel.total
            ) + ", " + Util.getSpeedText(downloadModel.speedInBytePerMs)

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
    }

}