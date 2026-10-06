/*
 * 程序化生成模组贴图（16x16 PNG）。
 * FTB Quests 的贴图是 All Rights Reserved，不能复用，所以这里全部自绘。
 *
 * 用法：node tools/gen_textures.js
 */
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

/* ---------------- PNG 编码 ---------------- */

const CRC_TABLE = (() => {
    const t = new Int32Array(256);
    for (let n = 0; n < 256; n++) {
        let c = n;
        for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
        t[n] = c;
    }
    return t;
})();

function crc32(buf) {
    let crc = -1;
    for (let i = 0; i < buf.length; i++) crc = (crc >>> 8) ^ CRC_TABLE[(crc ^ buf[i]) & 0xff];
    return (crc ^ -1) >>> 0;
}

function chunk(type, data) {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length);
    const t = Buffer.from(type, 'ascii');
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(Buffer.concat([t, data])));
    return Buffer.concat([len, t, data, crc]);
}

function encodePNG(rgba, w, h) {
    const stride = w * 4;
    const raw = Buffer.alloc((stride + 1) * h);
    for (let y = 0; y < h; y++) {
        raw[y * (stride + 1)] = 0; // filter: none
        rgba.copy(raw, y * (stride + 1) + 1, y * stride, (y + 1) * stride);
    }
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(w, 0);
    ihdr.writeUInt32BE(h, 4);
    ihdr[8] = 8;  // bit depth
    ihdr[9] = 6;  // color type RGBA
    return Buffer.concat([
        Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
        chunk('IHDR', ihdr),
        chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
        chunk('IEND', Buffer.alloc(0)),
    ]);
}

/* ---------------- 画布 ---------------- */

class Canvas {
    constructor(w, h) {
        this.w = w;
        this.h = h;
        this.buf = Buffer.alloc(w * h * 4); // 默认全透明
    }

    set(x, y, c) {
        x |= 0;
        y |= 0;
        if (x < 0 || y < 0 || x >= this.w || y >= this.h) return;
        const i = (y * this.w + x) * 4;
        this.buf[i] = c[0];
        this.buf[i + 1] = c[1];
        this.buf[i + 2] = c[2];
        this.buf[i + 3] = c.length > 3 ? c[3] : 255;
    }

    get(x, y) {
        const i = (y * this.w + x) * 4;
        return [this.buf[i], this.buf[i + 1], this.buf[i + 2], this.buf[i + 3]];
    }

    rect(x, y, w, h, c) {
        for (let j = 0; j < h; j++) for (let i = 0; i < w; i++) this.set(x + i, y + j, c);
    }

    /** 只在非透明像素上着色（保留形状轮廓） */
    rectMasked(x, y, w, h, c) {
        for (let j = 0; j < h; j++) {
            for (let i = 0; i < w; i++) {
                const px = x + i, py = y + j;
                if (px < 0 || py < 0 || px >= this.w || py >= this.h) continue;
                if (this.get(px, py)[3] === 0) continue;
                this.set(px, py, c);
            }
        }
    }

    /** 整体压暗/提亮，用于生成 _on 变体 */
    shade(factor) {
        const out = new Canvas(this.w, this.h);
        out.buf = Buffer.from(this.buf);
        for (let i = 0; i < out.buf.length; i += 4) {
            if (out.buf[i + 3] === 0) continue;
            for (let k = 0; k < 3; k++) {
                out.buf[i + k] = Math.max(0, Math.min(255, Math.round(out.buf[i + k] * factor)));
            }
        }
        return out;
    }

    save(file) {
        fs.mkdirSync(path.dirname(file), { recursive: true });
        fs.writeFileSync(file, encodePNG(this.buf, this.w, this.h));
        console.log('  写出 ' + file);
    }
}

/* ---------------- 工具 ---------------- */

const mix = (a, b, t) => a.map((v, i) => Math.round(v + (b[i] - v) * t));

/** 确定性噪声，保证每次构建贴图一致 */
function mulberry32(seed) {
    return function () {
        seed |= 0;
        seed = (seed + 0x6d2b79f5) | 0;
        let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
        t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
}

/* ---------------- 调色板 ---------------- */

const METAL = {
    outline: [26, 27, 33],
    dark: [58, 63, 75],
    base: [84, 91, 107],
    light: [118, 127, 146],
    bright: [154, 164, 184],
};

const AMBER = { dark: [140, 78, 16], base: [232, 163, 61], glow: [255, 214, 130] };
const CYAN = { dark: [16, 96, 92], base: [61, 202, 186], glow: [150, 245, 232] };
const VIOLET = { dark: [58, 38, 112], base: [124, 92, 226], glow: [186, 162, 255] };

/* ---------------- 基础金属面板 ---------------- */

function metalBase(seed) {
    const c = new Canvas(16, 16);
    const rnd = mulberry32(seed);

    // 主体渐变：上亮下暗
    for (let y = 1; y < 15; y++) {
        const t = (y - 1) / 13;
        let col = mix(METAL.light, METAL.dark, t);
        for (let x = 1; x < 15; x++) {
            const n = (rnd() - 0.5) * 16;
            c.set(x, y, [col[0] + n, col[1] + n, col[2] + n].map((v) => Math.max(0, Math.min(255, v | 0))));
        }
    }

    // 斜面高光/阴影
    for (let x = 1; x < 15; x++) {
        c.set(x, 1, METAL.bright);
        c.set(x, 14, METAL.outline);
    }
    for (let y = 1; y < 15; y++) {
        c.set(1, y, METAL.bright);
        c.set(14, y, METAL.outline);
    }

    // 外描边
    for (let i = 0; i < 16; i++) {
        c.set(i, 0, METAL.outline);
        c.set(i, 15, METAL.outline);
        c.set(0, i, METAL.outline);
        c.set(15, i, METAL.outline);
    }

    // 四角铆钉
    for (const [rx, ry] of [[2, 2], [13, 2], [2, 13], [13, 13]]) {
        c.set(rx, ry, METAL.bright);
        c.set(rx + (rx < 8 ? 1 : -1), ry, METAL.base);
    }
    return c;
}

/** 内凹面板（画在 16x16 上，x/y/w/h 为面板范围） */
function recessedPanel(c, x, y, w, h, fill) {
    c.rect(x, y, w, h, METAL.outline);                       // 外框
    c.rect(x + 1, y + 1, w - 2, h - 2, METAL.dark);          // 内壁
    c.rect(x + 1, y + 1, w - 2, 1, METAL.outline);           // 顶部阴影
    c.rect(x + 1, y + 1, 1, h - 2, METAL.outline);
    if (fill) c.rect(x + 2, y + 2, w - 4, h - 4, fill);
}

/* ---------------- 正面：纯黑屏幕 ---------------- */

/**
 * 正面是一整块纯黑面板，当作"屏幕"用。
 * 任务图标和进度由 BoundBlockRenderer 画在上面，所以贴图本身不画任何东西。
 *
 * 目前 powered/lit 两种状态用的是同一张纯黑贴图。
 * 以后想给"通电/刚领到"加个视觉提示，改 _on 那张就行，blockstate 已经分好了。
 */
function blackFace() {
    const c = new Canvas(16, 16);
    c.rect(0, 0, 16, 16, [0, 0, 0]);
    return c;
}

/* ---------------- 侧面：金属 + 横向能量条 ---------------- */

function blockSide(seed, accent) {
    const c = metalBase(seed);
    // 上下各一条凹槽
    c.rect(2, 4, 12, 1, METAL.outline);
    c.rect(2, 11, 12, 1, METAL.outline);
    // 中段能量条纹
    c.rect(3, 7, 10, 2, accent.dark);
    for (let x = 4; x < 12; x += 3) c.rect(x, 7, 1, 2, accent.base);
    return c;
}

/* ---------------- 顶面：金属 + 中央徽记 ---------------- */

function blockTop(seed, accent) {
    const c = metalBase(seed);
    recessedPanel(c, 4, 4, 8, 8, null);
    // 中央菱形徽记
    const cx = 7.5, cy = 7.5;
    for (let y = 0; y < 8; y++) {
        for (let x = 0; x < 8; x++) {
            const d = Math.abs(x - 3.5) + Math.abs(y - 3.5);
            if (d < 2.5) c.set(4 + x, 4 + y, accent.base);
            else if (d < 3.5) c.set(4 + x, 4 + y, accent.dark);
        }
    }
    c.set(7, 7, accent.glow || accent.base);
    c.set(8, 8, accent.glow || accent.base);
    return c;
}

/* ---------------- 绑定器物品 ---------------- */

function binderItem() {
    const c = new Canvas(16, 16);

    // 机身
    c.rect(3, 2, 10, 12, METAL.outline);
    c.rect(4, 3, 8, 10, [42, 46, 58]);
    c.rect(4, 3, 8, 1, [70, 77, 94]);

    // 屏幕（外框 + 发光面）
    c.rect(5, 4, 6, 5, METAL.outline);
    c.rect(6, 5, 4, 3, VIOLET.dark);
    c.rect(6, 5, 4, 1, VIOLET.base);
    c.set(7, 6, VIOLET.glow);
    c.set(8, 6, VIOLET.glow);

    // 按键
    c.rect(5, 10, 2, 1, METAL.base);
    c.rect(8, 10, 3, 1, METAL.base);
    c.rect(5, 12, 6, 1, METAL.base);

    // 顶部发射头
    c.rect(6, 1, 4, 1, METAL.outline);
    c.rect(7, 0, 2, 1, VIOLET.base);

    // 右侧指示灯
    c.set(12, 4, AMBER.base);
    c.set(12, 6, CYAN.base);
    return c;
}

/* ---------------- 输出 ---------------- */

const BLOCK_DIR = 'E:/ftbquestauto/src/main/resources/assets/ftbqauto/textures/block';
const ITEM_DIR = 'E:/ftbquestauto/src/main/resources/assets/ftbqauto/textures/item';

console.log('生成方块贴图...');

blockTop(3001, AMBER).save(path.join(BLOCK_DIR, 'quest_submitter_top.png'));
blockSide(3002, AMBER).save(path.join(BLOCK_DIR, 'quest_submitter_side.png'));
blackFace().save(path.join(BLOCK_DIR, 'quest_submitter_front.png'));
blackFace().save(path.join(BLOCK_DIR, 'quest_submitter_front_on.png'));

blockTop(4001, CYAN).save(path.join(BLOCK_DIR, 'reward_claimer_top.png'));
blockSide(4002, CYAN).save(path.join(BLOCK_DIR, 'reward_claimer_side.png'));
blackFace().save(path.join(BLOCK_DIR, 'reward_claimer_front.png'));
blackFace().save(path.join(BLOCK_DIR, 'reward_claimer_front_on.png'));

console.log('生成物品贴图...');
binderItem().save(path.join(ITEM_DIR, 'quest_binder.png'));

console.log('完成。');
