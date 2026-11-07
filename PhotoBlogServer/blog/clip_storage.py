from django.conf import settings

from .models import Post


def _file_size(field_file):
    try:
        return field_file.size
    except OSError:  # DB에는 있는데 파일이 없는 경우
        return 0


def enforce_clip_storage_limit():
    """ID 10 기능 B 보관 정책: 저장된 클립 총용량이 CLIP_STORAGE_LIMIT_MB를 넘으면 오래된 클립부터 삭제한다.

    게시물·썸네일 이미지는 남기고 clip 파일만 지운 뒤 clip_expired=True로 표시한다.
    파일당 크기 제한은 두지 않는다. 만료 처리한 게시물 수를 돌려준다.
    """
    limit = int(settings.CLIP_STORAGE_LIMIT_MB * 1024 * 1024)
    posts = list(Post.objects.exclude(clip='').order_by('created_date'))
    sizes = {post.pk: _file_size(post.clip) for post in posts}
    total = sum(sizes.values())

    expired = 0
    for post in posts:
        if total <= limit:
            break
        total -= sizes[post.pk]
        post.clip.delete(save=False)
        post.clip = ''
        post.clip_expired = True
        post.save(update_fields=['clip', 'clip_expired'])
        expired += 1
    return expired
