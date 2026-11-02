from django.contrib.auth import get_user_model
from django.contrib.auth.decorators import login_required
from django.shortcuts import get_object_or_404, redirect, render
from django.utils import timezone
from rest_framework import viewsets

from .forms import PostForm
from .models import Post
from .serializers import PostSerializer


class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer

    def perform_create(self, serializer):
        # Android Client는 로그인 기능이 없어 request.user가 없다(AnonymousUser).
        # 로그인된 요청(예: 브라우저에서 세션 인증된 요청)이면 그 사용자를,
        # 아니면 DB에서 동적으로 찾은 기본 작성자를 사용한다.
        # 특정 PK를 하드코딩하지 않으므로 db.sqlite3가 없는 새 환경에서도 그대로 동작한다.
        if self.request.user.is_authenticated:
            serializer.save(author=self.request.user)
        else:
            serializer.save(author=self._resolve_default_author())

    @staticmethod
    def _resolve_default_author():
        User = get_user_model()
        author = User.objects.filter(is_superuser=True).order_by('id').first()
        if author is None:
            author = User.objects.order_by('id').first()
        if author is None:
            # 사용자가 하나도 없는 완전히 비어있는 DB에서도 업로드가 실패하지 않도록
            # 전용 계정을 자동 생성한다 (로그인 불가능한 시스템 계정, 비밀번호 없음).
            author = User.objects.create_user(username='mobile_uploader')
        return author


def post_list(request):
    posts = Post.objects.all().order_by('-created_date')
    return render(request, 'blog/post_list.html', {'posts': posts})


def post_detail(request, pk):
    post = get_object_or_404(Post, pk=pk)
    return render(request, 'blog/post_detail.html', {'post': post})


@login_required
def post_new(request):
    if request.method == 'POST':
        form = PostForm(request.POST, request.FILES)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm()
    return render(request, 'blog/post_edit.html', {'form': form})


@login_required
def post_edit(request, pk):
    post = get_object_or_404(Post, pk=pk)
    if request.method == 'POST':
        form = PostForm(request.POST, request.FILES, instance=post)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm(instance=post)
    return render(request, 'blog/post_edit.html', {'form': form})
