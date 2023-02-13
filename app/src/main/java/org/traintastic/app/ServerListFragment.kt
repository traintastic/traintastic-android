package org.traintastic.app

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.traintastic.app.databinding.ServerListBinding
import kotlin.math.roundToInt


class ServerListFragment : Fragment() {

    private var _binding: ServerListBinding? = null
    private val serversListViewModel by viewModels<ServersListViewModel> {
        ServersListViewModelFactory(requireContext().applicationContext)
    }
    private lateinit var swipeHelper: ItemTouchHelper

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = ServerListBinding.inflate(inflater, container, false)

        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels )// displayMetrics.density).toInt()//.dp
        val deleteIcon = resources.getDrawable(R.drawable.outline_delete_24, null)
        val deletePaint = Paint()
        deletePaint.style = Paint.Style.FILL
        deletePaint.color = resources.getColor(android.R.color.holo_red_light)

        swipeHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = true

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                val servers = ServersDataSource.getDataSource(requireContext().applicationContext.resources)
                val server = servers.getServer(viewHolder.adapterPosition) ?: return
                servers.removeServer(viewHolder.adapterPosition)

                view?.let {
                    Snackbar.make(
                        it,
                        resources.getString(R.string.deleted_x, server.hostname),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }

            override fun getSwipeDirs (recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                // Only allow deletion of manually added (persistent) servers
                val servers = ServersDataSource.getDataSource(requireContext().applicationContext.resources)
                val server = servers.getServer(viewHolder.adapterPosition) ?: return 0
                if (!server.persistent) return 0
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                 if (dX < 0) {

                     val margin = (16 * displayMetrics.density).roundToInt()
                     val top = viewHolder.itemView.top + (viewHolder.itemView.bottom - viewHolder.itemView.top - deleteIcon.intrinsicHeight) / 2
                     deleteIcon.bounds = Rect(
                        width - margin - deleteIcon.intrinsicWidth,
                        top,
                        width - margin,
                        top + deleteIcon.intrinsicHeight)

                     canvas.drawRect(
                    width + dX,
                        viewHolder.itemView.top.toFloat(),
                        width.toFloat(),
                        viewHolder.itemView.bottom.toFloat(),
                        deletePaint)

                    deleteIcon.draw(canvas)
                }

                super.onChildDraw(
                    canvas,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        })
        swipeHelper.attachToRecyclerView(binding.rvServers)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val linearLayoutManager = LinearLayoutManager(requireContext().applicationContext)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.rvServers.layoutManager = linearLayoutManager

        val serversAdapter = ServersAdaptor { server -> adapterOnClick(server) }
        binding.rvServers.adapter = serversAdapter

        serversListViewModel.serversLiveData.observe(this, {
            it?.let {
                serversAdapter.submitList(it as MutableList<Server>)
            }
        })

        binding.fabAddServer.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun adapterOnClick(server: Server) {
        //val intent = Intent(this, FlowerDetailActivity()::class.java)
        //intent.putExtra(FLOWER_ID, flower.id)
        //startActivity(intent)
    }
}