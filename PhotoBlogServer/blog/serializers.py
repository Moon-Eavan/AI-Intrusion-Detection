from datetime import timedelta

from django.utils import timezone
from rest_framework import serializers

from .models import MonitorConfig, Post


class PostSerializer(serializers.ModelSerializer):
    class Meta:
        model = Post
        fields = ['author', 'title', 'text', 'created_date', 'published_date', 'image']
        # author는 클라이언트가 직접 지정하지 않는다 (BlogImages.perform_create에서 서버가 결정).
        # 특정 DB의 PK 값에 의존하지 않기 위함이며, GET 응답에는 계속 포함된다.
        read_only_fields = ['author']


# Edge가 5초마다 heartbeat를 보내므로 15초(3회 누락) 안에 기록이 있으면 온라인으로 본다
EDGE_ONLINE_SECONDS = 15


def validate_class_list(value):
    if not isinstance(value, list) or not all(isinstance(c, str) for c in value):
        raise serializers.ValidationError('클래스명(문자열) 리스트여야 합니다.')
    return value


class MonitorConfigSerializer(serializers.ModelSerializer):
    # 온라인 판정은 서버 시각 기준으로 계산한다 (휴대폰과 서버 시계 차이 영향 없음)
    edge_online = serializers.SerializerMethodField()
    edge_seen_sec = serializers.SerializerMethodField()

    class Meta:
        model = MonitorConfig
        fields = ['enabled', 'target_classes', 'conf_threshold', 'cooldown_sec',
                  'available_classes', 'edge_last_seen', 'edge_online', 'edge_seen_sec']
        read_only_fields = ['available_classes', 'edge_last_seen']

    def get_edge_online(self, obj):
        if obj.edge_last_seen is None:
            return False
        return timezone.now() - obj.edge_last_seen <= timedelta(seconds=EDGE_ONLINE_SECONDS)

    def get_edge_seen_sec(self, obj):
        # 마지막 heartbeat 후 경과 초 (Android "n초 전" 표시용, 기록 없으면 null)
        if obj.edge_last_seen is None:
            return None
        return int((timezone.now() - obj.edge_last_seen).total_seconds())

    def validate_target_classes(self, value):
        validate_class_list(value)
        available = self.instance.available_classes if self.instance else []
        unknown = [c for c in value if available and c not in available]
        if unknown:
            raise serializers.ValidationError('Edge 모델에 없는 클래스: ' + ', '.join(unknown))
        return value
