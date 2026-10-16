package dcrustm.ecell.mobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.ui.home.InitialsAvatar

/**
 * The member's Google profile photo, drawn over their initials — so the initials show
 * while it loads, when it fails, or when there's no photo at all.
 */
@Composable
fun MemberAvatar(initials: String, photoUrl: String, size: Int = 36) {
    Box(modifier = Modifier.size(size.dp).clip(CircleShape)) {
        InitialsAvatar(initials = initials, size = size)
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
fun MemberAvatar(member: Member, size: Int = 36) {
    MemberAvatar(initials = member.initials, photoUrl = member.profilePhotoUrl, size = size)
}
