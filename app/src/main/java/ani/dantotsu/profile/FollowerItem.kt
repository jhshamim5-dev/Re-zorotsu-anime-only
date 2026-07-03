package ani.dantotsu.profile

import android.text.SpannableString
import android.view.View
import androidx.viewbinding.ViewBinding
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.databinding.ItemFollowerBinding
import ani.dantotsu.databinding.ItemFollowerGridBinding
import ani.dantotsu.loadImage
import com.xwray.groupie.viewbinding.BindableItem
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.getThemeColor
import ani.dantotsu.snackString
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.res.ColorStateList

class FollowerItem(
    private val grid: Boolean,
    private val id: Int,
    private val name: SpannableString,
    private val avatar: String?,
    private val banner: String?,
    private var isFollowing: Boolean?,
    private var isFollower: Boolean?,
    val clickCallback: (Int) -> Unit
) : BindableItem<ViewBinding>() {

    override fun bind(viewBinding: ViewBinding, position: Int) {
        if (grid) {
            val binding = viewBinding as ItemFollowerGridBinding
            binding.profileUserName.text = name
            avatar?.let { binding.profileUserAvatar.loadImage(it) }
            binding.root.setOnClickListener { clickCallback(id) }
        } else {
            val binding = viewBinding as ItemFollowerBinding
            binding.profileUserName.text = name
            avatar?.let { binding.profileUserAvatar.loadImage(it) }
            blurImage(binding.profileBannerImage, banner ?: avatar)
            binding.root.setOnClickListener { clickCallback(id) }
            updateFollowButton(binding)
        }
    }

    private fun updateFollowButton(binding: ItemFollowerBinding) {
        val context = binding.root.context
        val btn = binding.followIndicatorBtn
        
        if (id == Anilist.userid || Anilist.userid == null) {
            btn.visibility = View.GONE
            return
        }
        
        btn.visibility = View.VISIBLE
        
        val primary = context.getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val onPrimary = context.getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        val primaryContainer = context.getThemeColor(com.google.android.material.R.attr.colorPrimaryContainer)
        val onPrimaryContainer = context.getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer)
        val outline = context.getThemeColor(com.google.android.material.R.attr.colorOutline)
        
        when {
            isFollowing == true && isFollower == true -> {
                btn.text = context.getString(R.string.mutual)
                btn.backgroundTintList = ColorStateList.valueOf(primaryContainer)
                btn.setTextColor(onPrimaryContainer)
                btn.strokeWidth = 0
            }
            isFollowing == false && isFollower == true -> {
                btn.text = context.getString(R.string.follows_you)
                btn.backgroundTintList = ColorStateList.valueOf(primaryContainer)
                btn.setTextColor(onPrimaryContainer)
                btn.strokeWidth = 0
            }
            isFollowing == true && isFollower == false -> {
                btn.text = context.getString(R.string.following)
                btn.backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
                btn.setTextColor(primary)
                btn.strokeColor = ColorStateList.valueOf(outline)
                btn.strokeWidth = 1
            }
            else -> {
                btn.text = context.getString(R.string.follow)
                btn.backgroundTintList = ColorStateList.valueOf(primary)
                btn.setTextColor(onPrimary)
                btn.strokeWidth = 0
            }
        }
        
        btn.setOnClickListener {
            val activity = context as? AppCompatActivity ?: return@setOnClickListener
            activity.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val res = Anilist.mutation.toggleFollow(id)
                    if (res?.data?.toggleFollow != null) {
                        isFollowing = res.data.toggleFollow.isFollowing
                        isFollower = res.data.toggleFollow.isFollower
                        withContext(Dispatchers.Main) {
                            snackString(R.string.success)
                            updateFollowButton(binding)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackString(e.localizedMessage ?: "Error")
                    }
                }
            }
        }
    }

    override fun getLayout(): Int {
        return if (grid) R.layout.item_follower_grid else R.layout.item_follower
    }

    override fun initializeViewBinding(view: View): ViewBinding {
        return if (grid) ItemFollowerGridBinding.bind(view) else ItemFollowerBinding.bind(view)
    }
}