/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.BlendBall;
import com.boydti.fawe.object.brush.BlobBrush;
import com.boydti.fawe.object.brush.BrushSettings;
import com.boydti.fawe.object.brush.CatenaryBrush;
import com.boydti.fawe.object.brush.CircleBrush;
import com.boydti.fawe.object.brush.CommandBrush;
import com.boydti.fawe.object.brush.CopyPastaBrush;
import com.boydti.fawe.object.brush.ErodeBrush;
import com.boydti.fawe.object.brush.FlattenBrush;
import com.boydti.fawe.object.brush.HeightBrush;
import com.boydti.fawe.object.brush.LayerBrush;
import com.boydti.fawe.object.brush.LineBrush;
import com.boydti.fawe.object.brush.PopulateSchem;
import com.boydti.fawe.object.brush.RaiseBrush;
import com.boydti.fawe.object.brush.RecurseBrush;
import com.boydti.fawe.object.brush.ScatterBrush;
import com.boydti.fawe.object.brush.ScatterCommand;
import com.boydti.fawe.object.brush.ScatterOverlayBrush;
import com.boydti.fawe.object.brush.ShatterBrush;
import com.boydti.fawe.object.brush.SplatterBrush;
import com.boydti.fawe.object.brush.SplineBrush;
import com.boydti.fawe.object.brush.StencilBrush;
import com.boydti.fawe.object.brush.SurfaceSphereBrush;
import com.boydti.fawe.object.brush.SurfaceSpline;
import com.boydti.fawe.object.brush.heightmap.ScalableHeightMap;
import com.boydti.fawe.object.mask.IdMask;
import com.boydti.fawe.util.ColorUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.command.tool.brush.ButcherBrush;
import com.sk89q.worldedit.command.tool.brush.ClipboardBrush;
import com.sk89q.worldedit.command.tool.brush.CylinderBrush;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.command.tool.brush.HollowCylinderBrush;
import com.sk89q.worldedit.command.tool.brush.HollowSphereBrush;
import com.sk89q.worldedit.command.tool.brush.SmoothBrush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.InvalidUsageException;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Commands to set brush shape.
 */
@Command(aliases = {"brush", "br", "tool"},
        desc = "Commands to build and draw from far away. [More Info](https://git.io/vSPYf)"
)
public class BrushCommands extends MethodCommands {

    public BrushCommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    private BrushSettings get(CommandContext context) throws InvalidToolBindException {
        BrushSettings bs = new BrushSettings();
        bs.addPermissions(getPermissions());
        CommandLocals locals = context.getLocals();
        if (locals != null) {
            String args = (String) locals.get("arguments");
            if (args != null) {
                bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
            }
        }
        return bs;
    }


    @Command(
            aliases = {"blendball", "bb", "blend"},
            usage = "[radius=5]",
            desc = "Smooths and blends terrain",
            help = "Smooths and blends terrain\n" +
                    "Pic: https://i.imgur.com/cNUQUkj.png -> https://i.imgur.com/hFOFsNf.png",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.blendball")
    public BrushSettings blendBallBrush(Player player, LocalSession session, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context).setBrush(new BlendBall()).setSize(radius);
    }

    @Command(
            aliases = {"erode", "e"},
            usage = "[radius=5]",
            desc = "Erodes terrain",
            help = "Erodes terrain",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.erode")
    public BrushSettings erodeBrush(Player player, LocalSession session, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context).setBrush(new ErodeBrush()).setSize(radius);
    }

    @Command(
            aliases = {"pull"},
            usage = "[radius=5]",
            desc = "Pull terrain towards you",
            help = "Pull terrain towards you",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.pull")
    public BrushSettings pullBrush(Player player, LocalSession session, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context).setBrush(new RaiseBrush()).setSize(radius);
    }

    @Command(
            aliases = {"circle"},
            usage = "<pattern> [radius=5]",
            desc = "Creates a circle which revolves around your facing direction",
            help = "Creates a circle which revolves around your facing direction.\n" +
                    "Note: Decrease brush radius, and enabled visualization to assist with placement mid-air",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.sphere")
    public BrushSettings circleBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context).setBrush(new CircleBrush(player)).setSize(radius).setFill(fill);
    }

    @Command(
            aliases = {"recursive", "recurse", "r"},
            usage = "<pattern-to> [radius=5]",
            desc = "Set all connected blocks",
            help = "Set all connected blocks\n" +
                    "The -d flag Will apply in depth first order\n" +
                    "Note: Set a mask to recurse along specific blocks",
            min = 0,
            max = 3
    )
    @CommandPermissions("worldedit.brush.recursive")
    public BrushSettings recursiveBrush(Player player, LocalSession session, EditSession editSession, Pattern fill, @Optional("5") double radius, @Switch('d') boolean depthFirst, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new RecurseBrush(depthFirst))
                .setSize(radius)
                .setFill(fill)
                .setMask(new IdMask(editSession));
    }

    @Command(
            aliases = {"line", "l"},
            usage = "<pattern> [radius=0]",
            flags = "hsf",
            desc = "Create lines",
            help =
                    "Create lines.\n" +
                            "The -h flag creates only a shell\n" +
                            "The -s flag selects the clicked point after drawing\n" +
                            "The -f flag creates a flat line",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.line")
    public BrushSettings lineBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("0") double radius, @Switch('h') boolean shell, @Switch('s') boolean select, @Switch('f') boolean flat, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new LineBrush(shell, select, flat))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"spline", "spl", "curve"},
            usage = "<pattern>",
            desc = "Join multiple objects together in a curve",
            help = "Click to select some objects,click the same block twice to connect the objects.\n" +
                    "Insufficient brush radius, or clicking the the wrong spot will result in undesired shapes. The shapes must be simple lines or loops.\n" +
                    "Pic1: http://i.imgur.com/CeRYAoV.jpg -> http://i.imgur.com/jtM0jA4.png\n" +
                    "Pic2: http://i.imgur.com/bUeyc72.png -> http://i.imgur.com/tg6MkcF.png",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.brush.spline")
    public BrushSettings splineBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("25") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE.f(radius));
        return get(context)
                .setBrush(new SplineBrush(player, session))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"catenary", "cat", "gravityline", "saggedline"},
            usage = "<pattern> [length-factor=1.2] [size=0]",
            desc = "Create a hanging line between two points",
            help = "Create a hanging line between two points.\n" +
                    "The length-factor controls how long the line is\n" +
                    "The -h flag creates only a shell\n" +
                    "The -s flag selects the clicked point after drawing\n",
            min = 1,
            max = 3
    )
    @CommandPermissions("worldedit.brush.spline")
    public BrushSettings catenaryBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("1.2") @Range(min=1) double lengthFactor, @Optional("0") double radius, @Switch('h') boolean shell, @Switch('s') boolean select, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new CatenaryBrush(shell, select, lengthFactor))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"sspl", "sspline", "surfacespline"},
            usage = "<pattern> [size=0] [tension=0] [bias=0] [continuity=0] [quality=10]",
            desc = "Draws a spline (curved line) on the surface",
            help = "Create a spline on the surface\n" +
                    "Video: https://www.youtube.com/watch?v=zSN-2jJxXlM",
            min = 0,
            max = 6
    )
    @CommandPermissions("worldedit.brush.surfacespline") // 0, 0, 0, 10, 0,
    public BrushSettings surfaceSpline(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("0") double radius, @Optional("0") double tension, @Optional("0") double bias, @Optional("0") double continuity, @Optional("10") double quality, CommandContext context) throws WorldEditException {
        player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE.f(radius));
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new SurfaceSpline(tension, bias, continuity, quality))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"blob", "rock"},
            usage = "<pattern> [radius=10] [frequency=30] [amplitude=50]",
            flags = "h",
            desc = "Creates a distorted sphere",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.blob")
    public BrushSettings blobBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("10") Vector radius, @Optional("30") double frequency, @Optional("50") double amplitude, CommandContext context) throws WorldEditException {
        double max = MathMan.max(radius.getBlockX(), radius.getBlockY(), radius.getBlockZ());
        worldEdit.checkMaxBrushRadius(max);
        Brush brush = new BlobBrush(radius.divide(max), frequency / 100, amplitude / 100);
        return get(context)
                .setBrush(brush)
                .setSize(max)
                .setFill(fill);
    }

    @Command(
            aliases = {"sphere", "s"},
            usage = "<pattern> [radius=2]",
            flags = "h",
            desc = "Creates a sphere",
            help =
                    "Creates a sphere.\n" +
                            "The -h flag creates hollow spheres instead.",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.sphere")
    public BrushSettings sphereBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("2") double radius, @Switch('h') boolean hollow, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        Brush brush;
        if (hollow) {
            brush = new HollowSphereBrush();
        } else {
            brush = new SphereBrush();
        }
        if (fill instanceof BaseBlock) {
            BaseBlock block = (BaseBlock) fill;
            switch (block.getId()) {
                case BlockID.SAND:
                case BlockID.GRAVEL:
                    BBC.BRUSH_TRY_OTHER.send(player);
                    break;
            }
        }
        return get(context)
                .setBrush(brush)
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"shatter", "partition", "split"},
            usage = "<pattern> [radius=10] [count=10]",
            desc = "Creates random lines to break the terrain into pieces",
            help =
                    "Creates uneven lines separating terrain into multiple pieces\n" +
                            "Pic: https://i.imgur.com/2xKsZf2.png",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.shatter")
    public BrushSettings shatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("10") double radius, @Optional("10") int count, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new ShatterBrush(count))
                .setSize(radius)
                .setFill(fill)
                .setMask(new ExistingBlockMask(editSession));
    }

    @Command(
            aliases = {"stencil", "color"},
            usage = "<pattern> [radius=5] [file|#clipboard|imgur=null] [rotation=360] [yscale=1.0]",
            desc = "Use a height map to paint a surface",
            help =
                    "Use a height map to paint any surface.\n" +
                            "The -w flag will only apply at maximum saturation\n" +
                            "The -r flag will apply random rotation",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.stencil")
    public BrushSettings stencilBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale, @Switch('w') boolean onlyWhite, @Switch('r') boolean randomRotate, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        InputStream stream = getHeightmapStream(filename);
        HeightBrush brush;
        try {
            brush = new StencilBrush(stream, rotation, yscale, onlyWhite, filename.equalsIgnoreCase("#clipboard") ? session.getClipboard().getClipboard() : null);
        } catch (EmptyClipboardException ignore) {
            brush = new StencilBrush(stream, rotation, yscale, onlyWhite, null);
        }
        if (randomRotate) {
            brush.setRandomRotate(true);
        }
        return get(context)
                .setBrush(brush)
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"surface", "surf"},
            usage = "<pattern> [radius=5]",
            desc = "Use a height map to paint a surface",
            help =
                    "Use a height map to paint any surface.\n" +
                            "The -w flag will only apply at maximum saturation\n" +
                            "The -r flag will apply random rotation",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.surface")
    public BrushSettings surfaceBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context).setBrush(new SurfaceSphereBrush()).setFill(fill).setSize(radius);
    }

    @Command(
            aliases = {"scatter", "scat"},
            usage = "<pattern> [radius=5] [points=5] [distance=1]",
            desc = "Scatter a pattern on a surface",
            help =
                    "Set a number of blocks randomly on a surface each a certain distance apart.\n" +
                            " The -o flag will overlay the block\n" +
                            "Video: https://youtu.be/RPZIaTbqoZw?t=34s",
            flags = "o",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.scatter")
    public BrushSettings scatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, @Optional("5") double points, @Optional("1") double distance, @Switch('o') boolean overlay, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush;
        if (overlay) {
            brush = new ScatterOverlayBrush((int) points, (int) distance);
        } else {
            brush = new ScatterBrush((int) points, (int) distance);
        }
        return get(context)
                .setBrush(brush)
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"populateschematic", "populateschem", "popschem", "pschem", "ps"},
            usage = "<mask> <file|folder|url> [radius=30] [points=5]",
            desc = "Scatter a schematic on a surface",
            help =
                    "Chooses the scatter schematic brush.\n" +
                            "The -r flag will apply random rotation",
            flags = "r",
            min = 2,
            max = 4
    )
    @CommandPermissions("worldedit.brush.populateschematic")
    public BrushSettings scatterSchemBrush(Player player, EditSession editSession, LocalSession session, Mask mask, String clipboard, @Optional("30") double radius, @Optional("50") double density, @Switch('r') boolean rotate, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);


        try {
            ClipboardHolder[] clipboards = ClipboardFormat.SCHEMATIC.loadAllFromInput(player, player.getWorld().getWorldData(), clipboard, true);
            if (clipboards == null) {
                return null;
            }
            return get(context)
                    .setBrush(new PopulateSchem(mask, clipboards, (int) density, rotate))
                    .setSize(radius);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Command(
            aliases = {"layer"},
            usage = "<radius> [color|<pattern1> <patern2>...]",
            desc = "Replaces terrain with a layer.",
            help = "Replaces terrain with a layer.\n" +
                    "Example: /br layer 5 95:1 95:2 35:15 - Places several layers on a surface\n" +
                    "Pic: https://i.imgur.com/XV0vYoX.png",
            min = 0,
            max = 999
    )
    @CommandPermissions("worldedit.brush.layer")
    public BrushSettings surfaceLayer(Player player, EditSession editSession, LocalSession session, double radius, CommandContext args, CommandContext context) throws WorldEditException, InvalidUsageException {
        worldEdit.checkMaxBrushRadius(radius);
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(player);
        parserContext.setWorld(player.getWorld());
        parserContext.setSession(session);
        parserContext.setExtent(editSession);
        List<BaseBlock> blocks = new ArrayList<>();
        if (args.argsLength() < 2) {
            throw new InvalidUsageException(getCallable());
        }
        try {
            Color color = ColorUtil.parseColor(args.getString(1));
            char[] glassLayers = Fawe.get().getTextureUtil().getNearestLayer(color.getRGB());
            for (char layer : glassLayers) {
                blocks.add(FaweCache.CACHE_BLOCK[layer]);
            }
        } catch (IllegalArgumentException ignore) {
            for (int i = 1; i < args.argsLength(); i++) {
                String arg = args.getString(i);
                blocks.add(worldEdit.getBlockFactory().parseFromInput(arg, parserContext));
            }
        }
        return get(context)
                .setBrush(new LayerBrush(blocks.toArray(new BaseBlock[blocks.size()])))
                .setSize(radius);
    }

    @Command(
            aliases = {"splatter", "splat"},
            usage = "<pattern> [radius=5] [seeds=1] [recursion=5] [solid=true]",
            desc = "Splatter a pattern on a surface",
            help = "Sets a bunch of blocks randomly on a surface.\n" +
                    "Pic: https://i.imgur.com/hMD29oO.png\n" +
                    "Example: /br splatter stone,dirt 30 15\n" +
                    "Note: The seeds define how many splotches there are, recursion defines how large, solid defines whether the pattern is applied per seed, else per block.",
            min = 1,
            max = 5
    )
    @CommandPermissions("worldedit.brush.splatter")
    public BrushSettings splatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, @Optional("1") double points, @Optional("5") double recursion, @Optional("true") boolean solid, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new SplatterBrush((int) points, (int) recursion, solid))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"scmd", "scattercmd", "scattercommand", "scommand"},
            usage = "<scatter-radius> <points> <cmd-radius=1> <cmd1;cmd2...>",
            desc = "Run commands at random points on a surface",
            help =
                    "Run commands at random points on a surface\n" +
                            " - The scatter radius is the min distance between each point\n" +
                            " - Your selection will be expanded to the specified size around each point\n" +
                            " - Placeholders: {x}, {y}, {z}, {world}, {size}",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.scattercommand")
    public BrushSettings scatterCommandBrush(Player player, EditSession editSession, LocalSession session, double radius, double points, double distance, CommandContext args, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        return get(context)
                .setBrush(new ScatterCommand((int) points, (int) distance, args.getJoinedStrings(3)))
                .setSize(radius);
    }

    @Command(
            aliases = {"cylinder", "cyl", "c", "disk", "disc"},
            usage = "<pattern> [radius=2] [height=1]",
            flags = "h",
            desc = "Creates a cylinder",
            help =
                    "Creates a cylinder.\n" +
                            "The -h flag creates hollow cylinders instead.",
            min = 1,
            max = 3
    )
    @CommandPermissions("worldedit.brush.cylinder")
    public BrushSettings cylinderBrush(Player player, EditSession editSession, LocalSession session, Pattern fill,
                                       @Optional("2") double radius, @Optional("1") int height, @Switch('h') boolean hollow, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        worldEdit.checkMaxBrushRadius(height);
        BrushSettings settings = get(context);

        if (hollow) {
            settings.setBrush(new HollowCylinderBrush(height));
        } else {
            settings.setBrush(new CylinderBrush(height));
        }
        settings.setSize(radius)
                .setFill(fill);
        return settings;
    }

    @Command(
            aliases = {"clipboard"},
            usage = "",
            desc = "Choose the clipboard brush (Recommended: `/br copypaste`)",
            help =
                    "Chooses the clipboard brush.\n" +
                            "The -a flag makes it not paste air.\n" +
                            "Without the -p flag, the paste will appear centered at the target location. " +
                            "With the flag, then the paste will appear relative to where you had " +
                            "stood relative to the copied area when you copied it."
    )
    @CommandPermissions("worldedit.brush.clipboard")
    public BrushSettings clipboardBrush(Player player, LocalSession session, @Switch('a') boolean ignoreAir, @Switch('p') boolean usingOrigin, CommandContext context) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();

        Vector size = clipboard.getDimensions();

        worldEdit.checkMaxBrushRadius(size.getBlockX());
        worldEdit.checkMaxBrushRadius(size.getBlockY());
        worldEdit.checkMaxBrushRadius(size.getBlockZ());
        return get(context).setBrush(new ClipboardBrush(holder, ignoreAir, usingOrigin));
    }

    @Command(
            aliases = {"smooth"},
            usage = "[size=2] [iterations=4]",
            flags = "n",
            desc = "Smooths terrain (Recommended: `/br blendball`)",
            help =
                    "Chooses the terrain softener brush.\n" +
                            "The -n flag makes it only consider naturally occurring blocks.",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.brush.smooth")
    public BrushSettings smoothBrush(Player player, LocalSession session, EditSession editSession,
                                     @Optional("2") double radius, @Optional("4") int iterations, @Switch('n')
                                             boolean naturalBlocksOnly, CommandContext context) throws WorldEditException {

        worldEdit.checkMaxBrushRadius(radius);

        FawePlayer fp = FawePlayer.wrap(player);
        FaweLimit limit = Settings.IMP.getLimit(fp);
        iterations = Math.min(limit.MAX_ITERATIONS, iterations);

        return get(context)
                .setBrush(new SmoothBrush(iterations, naturalBlocksOnly))
                .setSize(radius);
    }

    @Command(
            aliases = {"ex", "extinguish"},
            usage = "[radius=5]",
            desc = "Shortcut fire extinguisher brush",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.ex")
    public BrushSettings extinguishBrush(Player player, LocalSession session, EditSession editSession, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        Pattern fill = (new BaseBlock(0));
        return get(context)
                .setBrush(new SphereBrush())
                .setSize(radius)
                .setFill(fill)
                .setMask(new BlockMask(editSession, new BaseBlock(BlockID.FIRE)));
    }

    @Command(
            aliases = {"gravity", "grav"},
            usage = "[radius=5]",
            flags = "h",
            desc = "Gravity brush",
            help =
                    "This brush simulates the affect of gravity.\n" +
                            "The -h flag makes it affect blocks starting at the world's max y, " +
                            "instead of the clicked block's y + radius.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.gravity")
    public BrushSettings gravityBrush(Player player, LocalSession session, @Optional("5") double radius, @Switch('h') boolean fromMaxY, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        return get(context)
                .setBrush(new GravityBrush(fromMaxY))
                .setSize(radius);
    }

    @Command(
            aliases = {"height", "heightmap"},
            usage = "[radius=5] [file|#clipboard|imgur=null] [rotation=0] [yscale=1.00]",
            flags = "h",
            desc = "Raise or lower terrain using a heightmap",
            help =
                    "This brush raises and lowers land.\n" +
                            " - The `-r` flag enables random off-axis rotation\n" +
                            " - The `-l` flag will work on snow layers\n" +
                            " - The `-s` flag disables smoothing\n" +
                            "Note: Use a negative yscale to reduce height\n" +
                            "Snow Pic: https://i.imgur.com/Hrzn0I4.png",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public BrushSettings heightBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale, @Switch('r') boolean randomRotate, @Switch('l') boolean layers, @Switch('s') boolean dontSmooth, CommandContext context) throws WorldEditException {
        return terrainBrush(player, session, radius, filename, rotation, yscale, false, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CONE, context);
    }

    @Command(
            aliases = {"cliff", "flatcylinder"},
            usage = "[radius=5] [file|#clipboard|imgur=null] [rotation=0] [yscale=1.00]",
            flags = "h",
            desc = "Cliff brush",
            help =
                    "This brush flattens terrain and creates cliffs.\n" +
                            " - The `-r` flag enables random off-axis rotation\n" +
                            " - The `-l` flag will work on snow layers\n" +
                            " - The `-s` flag disables smoothing",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public BrushSettings cliffBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale, @Switch('r') boolean randomRotate, @Switch('l') boolean layers, @Switch('s') boolean dontSmooth, CommandContext context) throws WorldEditException {
        return terrainBrush(player, session, radius, filename, rotation, yscale, true, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CYLINDER, context);
    }

    @Command(
            aliases = {"flatten", "flatmap", "flat"},
            usage = "[radius=5] [file|#clipboard|imgur=null] [rotation=0] [yscale=1.00]",
            flags = "h",
            help = "Flatten brush flattens terrain\n" +
                    " - The `-r` flag enables random off-axis rotation\n" +
                    " - The `-l` flag will work on snow layers\n" +
                    " - The `-s` flag disables smoothing",
            desc = "This brush raises or lowers land towards the clicked point",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public BrushSettings flattenBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale, @Switch('r') boolean randomRotate, @Switch('l') boolean layers, @Switch('s') boolean dontSmooth, CommandContext context) throws WorldEditException {
        return terrainBrush(player, session, radius, filename, rotation, yscale, true, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CONE, context);
    }

    private InputStream getHeightmapStream(String filename) {
        String filenamePng = (filename.endsWith(".png") ? filename : filename + ".png");
        File file = new File(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HEIGHTMAP + File.separator + filenamePng);
        if (!file.exists()) {
            if (!filename.equals("#clipboard") && filename.length() >= 7) {
                try {
                    URL url;
                    if (filename.startsWith("http")) {
                        url = new URL(filename);
                        if (!url.getHost().equals("i.imgur.com")) {
                            throw new FileNotFoundException(filename);
                        }
                    } else {
                        url = new URL("https://i.imgur.com/" + filenamePng);
                    }
                    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    return Channels.newInputStream(rbc);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (!filename.equalsIgnoreCase("#clipboard")) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private BrushSettings terrainBrush(Player player, LocalSession session, double radius, String filename, int rotation, double yscale, boolean flat, boolean randomRotate, boolean layers, boolean smooth, ScalableHeightMap.Shape shape, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        InputStream stream = getHeightmapStream(filename);
        HeightBrush brush;
        if (flat) {
            try {
                brush = new FlattenBrush(stream, rotation, yscale, layers, smooth, filename.equalsIgnoreCase("#clipboard") ? session.getClipboard().getClipboard() : null, shape);
            } catch (EmptyClipboardException ignore) {
                brush = new FlattenBrush(stream, rotation, yscale, layers, smooth, null, shape);
            }
        } else {
            try {
                brush = new HeightBrush(stream, rotation, yscale, layers, smooth, filename.equalsIgnoreCase("#clipboard") ? session.getClipboard().getClipboard() : null);
            } catch (EmptyClipboardException ignore) {
                brush = new HeightBrush(stream, rotation, yscale, layers, smooth, null);
            }
        }
        if (randomRotate) {
            brush.setRandomRotate(true);
        }
        return get(context)
                .setBrush(brush)
                .setSize(radius);
    }


    @Command(
            aliases = {"copypaste", "copy", "paste", "cp", "copypasta"},
            usage = "[depth=5]",
            desc = "Copy Paste brush",
            help = "Left click the base of an object to copy.\n" +
                    "Right click to paste\n" +
                    "The -r flag Will apply random rotation on paste\n" +
                    "The -a flag Will apply auto view based rotation on paste\n" +
                    "Note: Works well with the clipboard scroll action\n" +
                    "Video: https://www.youtube.com/watch?v=RPZIaTbqoZw",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.copy")
    public BrushSettings copy(Player player, LocalSession session, @Optional("5") double radius, @Switch('r') boolean randomRotate, @Switch('a') boolean autoRotate, CommandContext context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        player.print(BBC.getPrefix() + BBC.BRUSH_COPY.f(radius));

        return get(context)
                .setBrush(new CopyPastaBrush(player, session, randomRotate, autoRotate))
                .setSize(radius);
    }

    @Command(
            aliases = {"command", "cmd"},
            usage = "<radius> [cmd1;cmd2...]",
            desc = "Command brush",
            help =
                    "Run the commands at the clicked position.\n" +
                            " - Your selection will be expanded to the specified size around each point\n" +
                            " - Placeholders: {x}, {y}, {z}, {world}, {size}",

            min = 2,
            max = 99
    )
    @CommandPermissions("worldedit.brush.command")
    public BrushSettings command(Player player, LocalSession session, double radius, CommandContext args, CommandContext context) throws WorldEditException {
        String cmd = args.getJoinedStrings(1);
        return get(context)
                .setBrush(new CommandBrush(cmd, radius))
                .setSize(radius);
    }

    @Command(
            aliases = {"butcher", "kill"},
            usage = "[radius=5]",
            flags = "plangbtfr",
            desc = "Butcher brush",
            help = "Kills nearby mobs within the specified radius.\n" +
                    "Flags:\n" +
                    "  -p also kills pets.\n" +
                    "  -n also kills NPCs.\n" +
                    "  -g also kills Golems.\n" +
                    "  -a also kills animals.\n" +
                    "  -b also kills ambient mobs.\n" +
                    "  -t also kills mobs with name tags.\n" +
                    "  -f compounds all previous flags.\n" +
                    "  -r also destroys armor stands.\n" +
                    "  -l currently does nothing.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.butcher")
    public BrushSettings butcherBrush(Player player, LocalSession session, CommandContext args, CommandContext context) throws WorldEditException {
        LocalConfiguration config = worldEdit.getConfiguration();

        double radius = args.argsLength() > 0 ? args.getDouble(0) : 5;
        double maxRadius = config.maxBrushRadius;
        // hmmmm not horribly worried about this because -1 is still rather efficient,
        // the problem arises when butcherMaxRadius is some really high number but not infinite
        // - original idea taken from https://github.com/sk89q/worldedit/pull/198#issuecomment-6463108
        if (player.hasPermission("worldedit.butcher")) {
            maxRadius = Math.max(config.maxBrushRadius, config.butcherMaxRadius);
        }
        if (radius > maxRadius && maxRadius != -1) {
            BBC.TOOL_RADIUS_ERROR.send(player, maxRadius);
            return null;
        }

        CreatureButcher flags = new CreatureButcher(player);
        flags.fromCommand(args);

        return get(context)
                .setBrush(new ButcherBrush(flags))
                .setSize(radius);
    }

    public static Class<?> inject() {
        return BrushCommands.class;
    }
}