from django.apps import AppConfig


class BlogConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'blog'

    def ready(self):
        from . import signals  # noqa: F401  게시물 삭제 시 파일 정리 (ID 10 기능 B)
