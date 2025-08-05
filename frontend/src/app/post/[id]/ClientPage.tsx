"use client";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { components } from "@/lib/backend/apiV1/schema";
import client from "@/lib/backend/client";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";

export default function ClinetPage({
  post,
}: {
  post: components["schemas"]["PostWithContentDto"];
}) {
  const router = useRouter();

  const handleDelete = async () => {
    if (!confirm("정말 삭제하시겠습니까?")) {
      return;
    }

    const response = await client.DELETE("/api/v1/posts/{id}", {
      credentials: "include",
      params: {
        path: {
          id: post.id,
        },
      },
    });

    if (response.error) {
      alert(response.error.msg);
      return;
    }

    router.push("/post/list");
  };

  return (
    <div className="container p-4 mx-auto">
      <Card>
        <CardHeader>
          <CardTitle>
            <Badge variant="outline">{post.id}</Badge>
            <p className="text-4xl text-center">{post.title}</p>
          </CardTitle>
          <CardDescription className="sr-only"></CardDescription>
          <div className="flex justify-between">
            <div className="flex items-center gap-2">
              <Image
                className="w-10 h-10 rounded-full"
                src={post.authorProfileImgUrl}
                alt="프로필 이미지"
                width={80}
                height={80}
                quality={100}
              />
              <div>
                <p className="text-lg">{post.authorName}</p>
                <p className="text-sm text-gray-500">
                  {new Intl.DateTimeFormat("ko-KR", {
                    year: "numeric",
                    month: "2-digit",
                    day: "2-digit",
                    hour: "2-digit",
                    minute: "2-digit",
                    hourCycle: "h23",
                  })
                    .format(new Date(post.modifiedDate))
                    .replace(/\. /g, ". ")}
                </p>
              </div>
            </div>
            <div>
              {post.canActorHandle && (
                <div className="flex gap-4 justify-center p-4">
                  <Button>
                    <Link href={`/post/edit/${post.id}`}>수정</Link>
                  </Button>
                  <Button variant="destructive" onClick={handleDelete}>
                    삭제
                  </Button>
                </div>
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent className="min-h-[calc(100vh-500px)] p-4">
          <p>{post.content}</p>
        </CardContent>
      </Card>
    </div>
  );
}
