from django.db.models.signals import post_delete
from django.dispatch import receiver

from .models import Post


@receiver(post_delete, sender=Post)
def delete_post_files(sender, instance, **kwargs):
    """ID 10 기능 B: Django는 게시물을 지워도 업로드 파일을 남기므로 이미지·클립 파일을 함께 지운다."""
    for field_file in (instance.image, instance.clip):
        if field_file:
            field_file.delete(save=False)
