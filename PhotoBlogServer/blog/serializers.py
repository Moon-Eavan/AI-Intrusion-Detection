from rest_framework import serializers

from .models import Post


class PostSerializer(serializers.ModelSerializer):
    class Meta:
        model = Post
        fields = ['author', 'title', 'text', 'created_date', 'published_date', 'image']
        # author는 클라이언트가 직접 지정하지 않는다 (BlogImages.perform_create에서 서버가 결정).
        # 특정 DB의 PK 값에 의존하지 않기 위함이며, GET 응답에는 계속 포함된다.
        read_only_fields = ['author']
