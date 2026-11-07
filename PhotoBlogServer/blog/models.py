from django.conf import settings
from django.core.validators import MaxValueValidator, MinValueValidator
from django.db import models
from django.utils import timezone


class Post(models.Model):
    author = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    title = models.CharField(max_length=200)
    text = models.TextField()
    created_date = models.DateTimeField(default=timezone.now)
    published_date = models.DateTimeField(blank=True, null=True)
    image = models.ImageField(upload_to='blog_image/%Y/%m/%d/', blank=True)

    def publish(self):
        self.published_date = timezone.now()
        self.save()

    def __str__(self):
        return self.title


class MonitorConfig(models.Model):
    """ID 10 기능 A: 원격 감시 설정. 행 1개(pk=1)만 사용한다."""

    enabled = models.BooleanField(default=True)
    # 빈 리스트 = 전체 클래스 감시
    target_classes = models.JSONField(default=list, blank=True)
    conf_threshold = models.FloatField(
        default=0.5, validators=[MinValueValidator(0.25), MaxValueValidator(0.9)])
    cooldown_sec = models.PositiveIntegerField(
        default=0, validators=[MinValueValidator(0), MaxValueValidator(3600)])
    # 아래 두 필드는 Edge heartbeat로만 바뀐다
    available_classes = models.JSONField(default=list, blank=True)
    edge_last_seen = models.DateTimeField(blank=True, null=True)

    @classmethod
    def load(cls):
        config, _ = cls.objects.get_or_create(pk=1)
        return config

    def __str__(self):
        return 'MonitorConfig'
