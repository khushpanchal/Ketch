package com.khush.sample

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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
import com.ketch.DownloadState
import com.ketch.Ketch
import com.ketch.NotificationConfig
import com.khush.sample.databinding.FragmentMainBinding
import com.khush.sample.databinding.ItemFileBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File


class MainFragment : Fragment() {

    private lateinit var fragmentMainBinding: FragmentMainBinding
    private lateinit var adapter: FilesAdapter

    private lateinit var ketch: Ketch

    private val downloads = MutableStateFlow<List<DownloadModel>>(emptyList())

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
        ketch = Ketch.init(
            this.requireContext(),
            notificationConfig = NotificationConfig(
                enabled = true,
                smallIcon = R.drawable.ic_launcher_foreground
            )
        )
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
                val request = ketch.download(url = url, fileName = fileName)

                downloads.update { downloads ->
                    val newDownload = DownloadModel(
                        url = request.url,
                        path = request.path,
                        fileName = request.fileName,
                        tag = request.tag,
                        id = request.id,
                        status = Status.DEFAULT,
                        timeQueued = System.currentTimeMillis(),
                        progress = 0,
                        total = 0L,
                        speedInBytePerMs = 0f
                    )

                    downloads + newDownload
                }

                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        ketch.observeDownloadById(request.id).collect { state ->
                            downloads.update { downloads ->
                                val mutableDownloads = downloads.toMutableList()
                                val index = mutableDownloads.indexOfFirst { it.id == request.id }

                                if (index == -1) {
                                    return@update mutableDownloads
                                }

                                val oldDownload = mutableDownloads[index]

                                val newDownload = when (state) {
                                    DownloadState.Blocked -> oldDownload
                                    DownloadState.Cancel -> {
                                        oldDownload.copy(status = Status.CANCELLED)
                                    }

                                    is DownloadState.Error -> {
                                        oldDownload.copy(status = Status.FAILED)
                                    }

                                    is DownloadState.Progress -> {
                                        oldDownload.copy(
                                            status = Status.PROGRESS,
                                            total = state.length,
                                            speedInBytePerMs = state.speedInBytePerMs,
                                            progress = state.progress
                                        )
                                    }

                                    DownloadState.Queued -> {
                                        oldDownload.copy(status = Status.QUEUED)
                                    }

                                    is DownloadState.Started -> {
                                        oldDownload.copy(
                                            status = Status.STARTED,
                                            total = state.length
                                        )
                                    }

                                    DownloadState.Success -> {
                                        oldDownload.copy(status = Status.SUCCESS)
                                    }
                                }

                                mutableDownloads[index] = newDownload

                                mutableDownloads
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observer() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                downloads.collect {//observe from viewModel to survive configuration change
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
            binding.size.text = Util.getTimeLeftText(
                downloadModel.speedInBytePerMs,
                downloadModel.progress,
                downloadModel.total
            ) + ", " + Util.getSpeedText(downloadModel.speedInBytePerMs)


            if (downloadModel.status != Status.SUCCESS) {
                binding.cancelButton.visibility = View.VISIBLE
            } else {
                binding.cancelButton.visibility = View.GONE
            }

            binding.cancelButton.setOnClickListener {
                listener.onCancelClick(downloadModel)
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
    }

}